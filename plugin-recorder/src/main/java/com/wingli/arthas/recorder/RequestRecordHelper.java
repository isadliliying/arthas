package com.wingli.arthas.recorder;

import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.cvte.psd.foundation.Foundation;
import com.google.common.util.concurrent.RateLimiter;
import com.wingli.arthas.recorder.entity.HttpRequestInfo;
import com.wingli.arthas.recorder.entity.RequestRecordParam;
import com.wingli.arthas.recorder.util.HttpUtil;
import com.wingli.arthas.recorder.util.IPUtils;
import com.wingli.arthas.recorder.util.RequestRecordUtil;
import org.apache.catalina.connector.CoyoteInputStream;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 记录请求
 */
public class RequestRecordHelper {

    private static final Logger logger = LoggerFactory.getLogger(RequestRecordHelper.class);

    private static final int MAX_LENGTH = 40960;

    private static final String requestPostUrl = "https://study.test.seewo.com/api/study/minder/v1/tools/request/record";

    public static AtomicLong newestHeartbeatTime = new AtomicLong();

    public static AtomicLong counter = null;

    public static AtomicBoolean isRecording = new AtomicBoolean(false);

    private static ThreadPoolExecutor fileAsyncThreadPool = null;

    private static ThreadPoolExecutor dataAsyncThreadPool = null;

    private static final RateLimiter rateLimiter = RateLimiter.create(5d);

    private static ConcurrentHashMap<Long, String> traceUidMap = new ConcurrentHashMap<Long, String>();
    private static ConcurrentHashMap<Long, String> userUidUidMap = new ConcurrentHashMap<Long, String>();
    private static ConcurrentHashMap<Long, Long> httpStartTimeMap = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, HttpRequestInfo> httpRequestMap = new ConcurrentHashMap<Long, HttpRequestInfo>();
    private static ConcurrentHashMap<Long, Integer> httpInputStreamMap = new ConcurrentHashMap<Long, Integer>();
    private static ConcurrentHashMap<Long, List<byte[]>> httpInputStreamBytesMap = new ConcurrentHashMap<Long, List<byte[]>>();
    private static ConcurrentHashMap<Long, Integer> httpOutputStreamMap = new ConcurrentHashMap<Long, Integer>();
    private static ConcurrentHashMap<Long, List<byte[]>> httpOutputStreamBytesMap = new ConcurrentHashMap<Long, List<byte[]>>();

    private static final String ip = IPUtils.getLocalIP();

    /**
     * 关闭
     */
    public static void onShutdown() {
        isRecording.set(false);
        if (fileAsyncThreadPool != null) fileAsyncThreadPool.shutdown();
        if (dataAsyncThreadPool != null) dataAsyncThreadPool.shutdown();
        traceUidMap.clear();
        userUidUidMap.clear();
        httpStartTimeMap.clear();
        httpRequestMap.clear();
        httpInputStreamMap.clear();
        httpInputStreamBytesMap.clear();
        httpOutputStreamMap.clear();
        httpOutputStreamBytesMap.clear();

        fileAsyncThreadPool = null;
        dataAsyncThreadPool = null;
        counter = null;
    }

    /**
     * 启动
     */
    public static void onStart() {
        isRecording.set(true);
        fileAsyncThreadPool = fileOperateThreadPoolExecutor();
        dataAsyncThreadPool = dataOperateThreadPoolExecutor();
        counter = new AtomicLong();
    }

    public static long keepalive() {
        newestHeartbeatTime.set(System.currentTimeMillis());
        return counter.longValue();
    }

    public static void receiveDubboRequestEnd(Invoker invoker, Invocation invocation, Result result) {
        try {

            //1.service.method
            final String interfaceClz = invoker.getInterface().getCanonicalName();
            final String methodName = invocation.getMethodName();

            if (!needToRecordDubbo(interfaceClz, methodName)) {
                return;
            }


            //2.param
            Object[] args = invocation.getArguments();

            Object[] generalizeArgs = PojoUtils.generalize(args);
            final String arrStr = new JSONArray(Arrays.asList(generalizeArgs)).toJSONString();

            //3.attachments
            Map<String, String> attachments = invocation.getAttachments();
            final String attachmentsStr = JSON.toJSONString(attachments);

            //write file
            fileAsyncThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        long current = counter.addAndGet(1);

                        String env = Foundation.server().getEnvType();
                        String appId = Foundation.app().getAppId();
                        RequestRecordParam param = new RequestRecordParam();
                        param.setTaskName(getTaskName());
                        param.setEnv(env);
                        param.setAppId(appId);
                        param.setIpAddr(ip);
                        param.setRequestType("dubbo");
                        param.setDubboInterface(interfaceClz);
                        param.setDubboMethod(methodName);
                        param.setDubboRequestArgs(arrStr);
                        param.setDubboRequestAttachments(attachmentsStr);
                        Map<String, String> headers = new HashMap<String, String>();
                        headers.put("Content-Type", "application/json");
                        HttpUtil.invokePostBody(requestPostUrl, headers, JSON.toJSONString(param));
                        stat(param, current);
                    } catch (Exception e) {
                        logger.error("write file err.", e);
                    }
                }
            });

        } catch (Throwable t) {
            logger.error("receiveDubboRequest err.", t);
        }

    }

    /**
     * 记录request
     */
    public static void recordHttpRequest(Object[] args) {
        if (!(args[1] instanceof Request && args[3] instanceof Response)) {
            return;
        }
        Request request = (Request) args[1];
        Response response = (Response) args[3];
        long threadId = Thread.currentThread().getId();
        try {
            //记录outputStream
            httpOutputStreamMap.put(threadId, response.getOutputStream().hashCode());
            httpOutputStreamBytesMap.remove(threadId);
            //记录InputStream
            httpInputStreamMap.put(threadId, request.getInputStream().hashCode());
            httpInputStreamBytesMap.remove(threadId);

            HttpRequestInfo httpRequestInfo = RequestRecordUtil.buildRequestInfo(request);
            if (!needToRecordHttp(httpRequestInfo)) {
                clear(threadId);
                return;
            }

            httpRequestMap.put(threadId, httpRequestInfo);
            httpStartTimeMap.put(threadId, System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("recordHttpRequest err.", e);
        }
    }

    /**
     * 记录中间的一些值，主要是thread local （插桩接口）
     * 这个没有异步的空间了，而且这个运行也特别快，没必要异步其实
     */
    public static void recordCommonTmp() {
        try {
            long threadId = Thread.currentThread().getId();

            HttpRequestInfo httpRequestInfo = httpRequestMap.get(threadId);
            if (httpRequestInfo == null) {
                return;
            }

            String traceUid = RequestRecordUtil.getTraceUid();
            String userUid = RequestRecordUtil.getUserUid();
            traceUidMap.put(threadId, traceUid);
            userUidUidMap.put(threadId, userUid);
        } catch (Throwable t) {
            logger.error("recordCommonTmp err.", t);
        }
    }

    /**
     * 记录response （插桩接口）
     * 主要是获取response里边的一些信息
     */
    public static void recordHttpResponse(Object[] args) {
        try {
            if (!(args[1] instanceof HttpServletResponse)) {
                return;
            }
            final HttpServletResponse resp = (HttpServletResponse) args[1];
            long threadId = Thread.currentThread().getId();

            final HttpRequestInfo httpRequestInfo = httpRequestMap.get(threadId);
            if (httpRequestInfo == null) {
                return;
            }

            //final ResponseInfo httpResponseInfo = RequestRecordUtil.buildResponseInfo(resp);

            List<byte[]> requestByteList = null;
            //List<byte[]> responseByteList = null;
            int takeTime = 0;

            String traceUid = null;
            String userUid = null;
            try {
                requestByteList = httpInputStreamBytesMap.get(threadId) != null ? httpInputStreamBytesMap.get(threadId) : Collections.<byte[]>emptyList();
                //responseByteList = httpOutputStreamBytesMap.get(threadId) != null ? httpOutputStreamBytesMap.get(threadId) : Collections.<byte[]>emptyList();
                //common
                traceUid = traceUidMap.get(threadId) != null ? traceUidMap.get(threadId) : "";
                userUid = userUidUidMap.get(threadId) != null ? userUidUidMap.get(threadId) : "";
                takeTime = (int) (System.currentTimeMillis() - (httpStartTimeMap.get(threadId) != null ? httpStartTimeMap.get(threadId) : System.currentTimeMillis()));
            } finally {
                clear(threadId);
            }

            //请求量大的情况下，有可能会出现并发问题
            final List<byte[]> finalRequestByteList = requestByteList;
            //final List<byte[]> finalResponseByteList = responseByteList;
            final int finalTakeTime = takeTime;
            final String finalTraceUid = traceUid;
            final String finalUserUid = userUid;
            fileAsyncThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        long current = counter.addAndGet(1);

                        //fill request body
                        byte[] requestBytes = flattenBytes(finalRequestByteList);
                        if (StringUtils.isNotBlank(httpRequestInfo.getEncode())) {
                            httpRequestInfo.setBody(new String(requestBytes, httpRequestInfo.getEncode()));
                        } else {
                            httpRequestInfo.setBody(new String(requestBytes));
                        }

                        //fill response body
                        //byte[] responseBytes = flattenBytes(finalResponseByteList);
                        //httpResponseInfo.setBody(new String(responseBytes, resp.getCharacterEncoding()));

                        String token = findTokenByHttpRequest(httpRequestInfo);
                        String headerStr = JSON.toJSONString(httpRequestInfo.getHeaders());
                        String env = Foundation.server().getEnvType();
                        String appId = Foundation.app().getAppId();
                        RequestRecordParam param = new RequestRecordParam();
                        param.setTaskName(getTaskName());
                        param.setEnv(env);
                        param.setAppId(appId);
                        param.setIpAddr(ip);
                        param.setRequestType("http");
                        param.setHttpRequestMethod(httpRequestInfo.getMethod());
                        param.setHttpRequestUrl(httpRequestInfo.getFullUrl());
                        param.setHttpRequestBody(httpRequestInfo.getBody());
                        param.setHttpRequestHeaders(headerStr);
                        param.setHttpTokenUid(token);
                        param.setHttpTraceUid(finalTraceUid);
                        param.setHttpUserUid(finalUserUid);
                        Map<String, String> headers = new HashMap<String, String>();
                        headers.put("Content-Type", "application/json");
                        HttpUtil.invokePostBody(requestPostUrl, headers, JSON.toJSONString(param));

                        stat(param, current);
                    } catch (Exception e) {
                        logger.error("write file err.", e);
                    }
                }
            });

        } catch (Throwable t) {
            logger.error("recordResponse err.", t);
        }

    }

    private static String getTaskName(){
        return System.getProperty("arthas.recorder.task.name", "undefine");
    }

    /**
     * 统计信息
     * 预留接口
     */
    public static void stat(RequestRecordParam param, long current) {

    }


    /**
     * 添加request body
     */
    public static void addHttpRequestByte(CoyoteInputStream inputStream, final byte[] bytes) {
        final long threadId = Thread.currentThread().getId();
        int inputStreamHashCode = httpInputStreamMap.get(threadId) != null ? httpInputStreamMap.get(threadId) : 0;
        if (inputStreamHashCode != inputStream.hashCode()) {
            return;
        }
        dataAsyncThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<byte[]> byteList = httpInputStreamBytesMap.get(threadId) != null ? httpInputStreamBytesMap.get(threadId) : new LinkedList<byte[]>();
                    int nextBytesSize = getBytesListSize(byteList) + bytes.length;
                    //做一下长度限制
                    if (nextBytesSize > MAX_LENGTH) {
                        return;
                    }
                    byteList.add(bytes);
                    httpInputStreamBytesMap.put(threadId, byteList);
                    logger.info("input space:" + calculateBytes(httpInputStreamBytesMap));
                } catch (Exception e) {
                    logger.error("save byte err.", e);
                }

            }
        });
    }

    /**
     * 添加response body
     */
    public static void addHttpResponseByte(CoyoteOutputStream outputStream, final byte[] bytes) {
        final long threadId = Thread.currentThread().getId();
        int outputStreamHashCode = httpOutputStreamMap.get(threadId) != null ? httpOutputStreamMap.get(threadId) : 0;
        if (outputStreamHashCode != outputStream.hashCode()) {
            return;
        }
        dataAsyncThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<byte[]> byteList = httpOutputStreamBytesMap.get(threadId) != null ? httpOutputStreamBytesMap.get(threadId) : new LinkedList<byte[]>();
                    //做一下长度限制
                    int nextBytesSize = getBytesListSize(byteList) + bytes.length;
                    if (nextBytesSize > MAX_LENGTH) {
                        return;
                    }
                    byteList.add(bytes);
                    httpOutputStreamBytesMap.put(threadId, byteList);
                    logger.info("output space:" + calculateBytes(httpOutputStreamBytesMap));
                } catch (Exception e) {
                    logger.error("save byte err.", e);
                }

            }
        });
    }

    /**
     * 判断是否需要记录
     */
    public static boolean needToRecord(CoyoteInputStream inputStream) {
        long threadId = Thread.currentThread().getId();
        int inputStreamHashCode = httpInputStreamMap.get(threadId) != null ? httpInputStreamMap.get(threadId) : 0;
        return inputStreamHashCode == inputStream.hashCode() && needToRecord();
    }

    /**
     * 判断是否需要记录
     */
    public static boolean needToRecord(CoyoteOutputStream outputStream) {
        long threadId = Thread.currentThread().getId();
        int outputStreamHashCode = httpOutputStreamMap.get(threadId) != null ? httpOutputStreamMap.get(threadId) : 0;
        return outputStreamHashCode == outputStream.hashCode() && needToRecord();
    }

    /**
     * 判断是否需要记录
     */
    private static boolean needToRecordHttp(HttpRequestInfo httpRequestInfo) {

        String regex = System.getProperty("arthas.recorder.http", "").trim();
        if (regex.isEmpty() || countAlreadyReach()) {
            return false;
        }

        String method = httpRequestInfo.getMethod().toUpperCase();
        String url = httpRequestInfo.getUrlWithoutQuery();
        //流量记录接口，避免循环
        if (url.endsWith("/v1/tools/request/record")){
            return false;
        }
        String param = method + " " + url;
        if (!Pattern.matches(regex, param)) {
            return false;
        }

        //ignore health check
        if (url.equals("/")) {
            return false;
        }

        return needToRecord();
    }

    private static boolean needToRecordDubbo(String dubboInterface, String dubboMethod) {
        String regex = System.getProperty("arthas.recorder.dubbo", "").trim();
        if (regex.isEmpty() || countAlreadyReach()) {
            return false;
        }
        String param = dubboInterface + " " + dubboMethod;
        if (!Pattern.matches(regex, param) && needToRecord()) {
            return false;
        }

        return needToRecord();
    }


    /**
     * 判断是否需要记录
     */
    private static boolean needToRecord() {

        if (!isRecording.get()) return false;

        //判断心跳
//        if (System.currentTimeMillis() - newestHeartbeatTime.get() > 10000) {
//            return false;
//        }

        //判断数量
        long currentCount = counter.longValue();
        String limitStr = System.getProperty("arthas.recorder.limit", "0").trim().toUpperCase();
        int limit = Integer.parseInt(limitStr);
        return currentCount < limit && rateLimiter.tryAcquire();
    }

    private static boolean countAlreadyReach() {
        long currentCount = counter.longValue();
        String limitStr = System.getProperty("arthas.recorder.limit", "0").trim().toUpperCase();
        int limit = Integer.parseInt(limitStr);
        return currentCount >= limit;
    }

    /**
     * 清除线程数据
     */
    private static void clear(long threadId) {
        traceUidMap.remove(threadId);
        userUidUidMap.remove(threadId);
        httpStartTimeMap.remove(threadId);
        httpRequestMap.remove(threadId);
        httpInputStreamMap.remove(threadId);
        httpInputStreamBytesMap.remove(threadId);
        httpOutputStreamMap.remove(threadId);
        httpOutputStreamBytesMap.remove(threadId);
    }

    /**
     * 从header中解析出token
     */
    private static String findTokenByHttpRequest(HttpRequestInfo httpRequestInfo) {
        if (httpRequestInfo.getHeaders() == null) return "";
        for (Pair<String, String> header : httpRequestInfo.getHeaders()) {
            if ("x-auth-token".equals(header.getKey())) {
                return header.getValue();
            }
        }
        return "";
    }

    /**
     * 获取当前字节长度
     */
    private static int getBytesListSize(List<byte[]> bytesList) {
        int size = 0;
        for (byte[] bytes : bytesList) {
            size += bytes.length;
        }
        return size;
    }

    /**
     * 计算占用空间
     */
    private static int calculateBytes(ConcurrentHashMap<Long, List<byte[]>> map) {
        int size = 0;
        Enumeration<List<byte[]>> it = map.elements();
        while (it.hasMoreElements()) {
            List<byte[]> byteList = it.nextElement();
            for (byte[] bytes : byteList) {
                size += bytes.length;
            }
        }
        return size;
    }

    /**
     * 展开字节数组
     */
    private static byte[] flattenBytes(List<byte[]> bytesList) {
        int size = getBytesListSize(bytesList);
        byte[] allByteArray = new byte[size];
        int curIdx = 0;
        for (byte[] bytes : bytesList) {
            System.arraycopy(bytes, 0, allByteArray, curIdx, bytes.length);
            curIdx += bytes.length;
        }
        return allByteArray;
    }

    public static ThreadPoolExecutor fileOperateThreadPoolExecutor() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(100);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 12, 30, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    public static ThreadPoolExecutor dataOperateThreadPoolExecutor() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(100);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 12, 30, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

}
