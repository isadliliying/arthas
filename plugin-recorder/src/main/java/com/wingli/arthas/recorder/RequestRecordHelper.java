package com.wingli.arthas.recorder;

import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.util.concurrent.RateLimiter;
import com.wingli.arthas.recorder.entity.DubboRequestFlowPo;
import com.wingli.arthas.recorder.entity.HttpRequestFlowPo;
import com.wingli.arthas.recorder.entity.HttpRequestInfo;
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
import java.io.File;
import java.io.FileWriter;
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

            if (!needToRecordDubbo(interfaceClz,methodName)){
                return;
            }

            final DubboRequestFlowPo dubboRequestFlowPo = new DubboRequestFlowPo();
            dubboRequestFlowPo.setDubbo_interface(interfaceClz);
            dubboRequestFlowPo.setDubbo_method(methodName);

            //2.param
            Object[] args = invocation.getArguments();
            dubboRequestFlowPo.setDubbo_request_args(JSON.toJSONString(args));

            Object[] generalizeArgs = PojoUtils.generalize(args);
            String arrStr = new JSONArray(Arrays.asList(generalizeArgs)).toJSONString();
            dubboRequestFlowPo.setDubbo_request_args(arrStr);

            //3.attachments
            Map<String, String> attachments = invocation.getAttachments();
            dubboRequestFlowPo.setDubbo_request_attachments(JSON.toJSONString(attachments));

            //write file
            fileAsyncThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        long current = counter.addAndGet(1);

                        String fileName = System.nanoTime() + "-" + counter + "-" + interfaceClz + "-" + methodName + "-" + ".json";
                        String dir = RequestRecordUtil.getRecordsDir();
                        String path = dir + fileName;
                        {
                            File file = new File(dir);
                            file.mkdirs();
                        }
                        {
                            File file = new File(path);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                        }
                        {
                            FileWriter fileWriter = new FileWriter(path, true);
                            fileWriter.append(JSON.toJSONString(dubboRequestFlowPo));
                            fileWriter.close();
                        }
                        stat(null, dubboRequestFlowPo, current);
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
            final HttpRequestFlowPo httpRequestFlowPo = new HttpRequestFlowPo();

            try {
                requestByteList = httpInputStreamBytesMap.get(threadId) != null ? httpInputStreamBytesMap.get(threadId) : Collections.<byte[]>emptyList();
                //responseByteList = httpOutputStreamBytesMap.get(threadId) != null ? httpOutputStreamBytesMap.get(threadId) : Collections.<byte[]>emptyList();
                //common
                httpRequestFlowPo.setTrace_uid(traceUidMap.get(threadId) != null ? traceUidMap.get(threadId) : "");
                httpRequestFlowPo.setUser_uid(userUidUidMap.get(threadId) != null ? userUidUidMap.get(threadId) : "");
                takeTime = (int) (System.currentTimeMillis() - (httpStartTimeMap.get(threadId) != null ? httpStartTimeMap.get(threadId) : System.currentTimeMillis()));

            } finally {
                clear(threadId);
            }

            //请求量大的情况下，有可能会出现并发问题
            final List<byte[]> finalRequestByteList = requestByteList;
            //final List<byte[]> finalResponseByteList = responseByteList;
            final int finalTakeTime = takeTime;
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

                        //http
                        httpRequestFlowPo.setHttp_token_uid(token);
                        httpRequestFlowPo.setHttp_request_method(httpRequestInfo.getMethod());
                        httpRequestFlowPo.setHttp_request_url(httpRequestInfo.getFullUrl());
                        httpRequestFlowPo.setHttp_request_headers(JSON.toJSONString(httpRequestInfo.getHeaders()));
                        httpRequestFlowPo.setHttp_request_body(httpRequestInfo.getBody());

                        String userUid = httpRequestFlowPo.getUser_uid();
                        if (StringUtils.isBlank(userUid)) {
                            userUid = "unknown";
                        }
                        String fileName = System.nanoTime() + "-" + counter + "-" + httpRequestInfo.getMethod() + "-" + httpRequestInfo.getUrlWithoutQuery().replace("/", "|") + "-" + userUid + ".json";
                        String dir = RequestRecordUtil.getRecordsDir();
                        String path = dir + fileName;
                        {
                            File file = new File(dir);
                            file.mkdirs();
                        }
                        {
                            File file = new File(path);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                        }
                        {
                            FileWriter fileWriter = new FileWriter(path, true);
                            fileWriter.append(JSON.toJSONString(httpRequestFlowPo));
                            fileWriter.close();
                        }
                        stat(httpRequestFlowPo, null, current);
                    } catch (Exception e) {
                        logger.error("write file err.", e);
                    }
                }
            });

        } catch (Throwable t) {
            logger.error("recordResponse err.", t);
        }

    }

    /**
     * 统计信息
     * 预留接口
     */
    public static void stat(HttpRequestFlowPo httpRequestFlowPo, DubboRequestFlowPo dubboRequestFlowPo, long current) {

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
        String param = method+" "+url;
        if (!Pattern.matches(regex,param)) {
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
        if (regex.isEmpty() || countAlreadyReach()){
            return false;
        }
        String param = dubboInterface+" "+dubboMethod;
        if (!Pattern.matches(regex,param) && needToRecord()) {
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

    private static boolean countAlreadyReach(){
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
