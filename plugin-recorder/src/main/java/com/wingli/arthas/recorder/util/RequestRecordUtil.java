package com.wingli.arthas.recorder.util;

import com.alibaba.fastjson.JSON;
import com.seewo.honeycomb.log.LogContextHolder;
import com.wingli.arthas.recorder.entity.HttpRequestInfo;
import com.wingli.arthas.recorder.entity.ResponseInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.*;

public class RequestRecordUtil {

    private static final Logger logger = LoggerFactory.getLogger(RequestRecordUtil.class);

    private static final String CURL_FORMAT_HEADER = "-H \"%1$s:%2$s\"";
    private static final String CURL_FORMAT_METHOD = "-X %1$s";
    private static final String CURL_FORMAT_BODY = "-d \"%1$s\"";
    private static final String CURL_FORMAT_URL = "\"%1$s\"";


    /**
     * 构建回放脚本
     */
    public static String buildHttpReply(HttpRequestInfo httpRequestInfo) {
        try {
            List<String> parts = new ArrayList<String>();
            parts.add("curl");

            //method
            parts.add(String.format(CURL_FORMAT_METHOD, httpRequestInfo.getMethod().toUpperCase()));

            String realUrl = httpRequestInfo.getFullUrl();

            //header
            for (Pair<String, String> header : httpRequestInfo.getHeaders()) {
                //移除accept-encoding，因为可能会返回gzip压缩
                if ("accept-encoding".equals(header.getKey())) {
                    parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-origin-accept-encoding"), transfer(header.getValue())));
                    continue;
                }
                //移除content-type，不然修改内容的时候，很容易忽略所以还不如直接改掉好了
                if ("content-length".equals(header.getKey())) {
                    parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-origin-content-length"), transfer(header.getValue())));
                    continue;
                }
                //重写url
                if ("x-forwarded-url".equals(header.getKey())) {
                    realUrl = header.getValue();
                    parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-origin-forwarded-url"), transfer(header.getValue())));
                    continue;
                }
                parts.add(String.format(CURL_FORMAT_HEADER, transfer(header.getKey()), transfer(header.getValue())));
            }
            //特殊标记
            parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-from-repeat"), "true"));

            //body
            String body = httpRequestInfo.getBody();
            if (StringUtils.isNotBlank(body)) {
                parts.add(String.format(CURL_FORMAT_BODY, transfer(body)));
            }

            //url
            parts.add(String.format(CURL_FORMAT_URL, transfer(realUrl)));

            return StringUtils.join(parts, " \\\r\n");
        } catch (Exception e) {
            logger.error("buildCurlByRequestInfo err. requestInfo:{}", JSON.toJSONString(httpRequestInfo), e);
        }
        return "";
    }

    /**
     * 构建request
     */
    public static HttpRequestInfo buildRequestInfo(ServletRequest req) {
        HttpRequestInfo httpRequestInfo = new HttpRequestInfo();
        try {
            if (req instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) req;

                //获取完整url
                String url = ((HttpServletRequest) req).getRequestURL().toString();
                String queryString = ((HttpServletRequest) req).getQueryString();
                URI uri = new URI(url);
                httpRequestInfo.setUrlWithoutQuery(uri.getRawPath());
                if (StringUtils.isBlank(queryString)) {
                    httpRequestInfo.setFullUrl(url);
                } else {
                    httpRequestInfo.setFullUrl(url + "?" + queryString);
                }

                //获取headers
                List<Pair<String, String>> headerList = new LinkedList<Pair<String, String>>();
                Enumeration<String> enumeration = request.getHeaderNames();
                while (enumeration.hasMoreElements()) {
                    String headerKey = enumeration.nextElement();
                    String headerValue = request.getHeader(headerKey);
                    Pair<String, String> pair = new MutablePair<String, String>(headerKey, headerValue);
                    headerList.add(pair);
                }
                httpRequestInfo.setHeaders(headerList);

                //获取method
                httpRequestInfo.setMethod(request.getMethod());

                //记录encode
                httpRequestInfo.setEncode(request.getCharacterEncoding());
            }

        } catch (Exception e) {
            logger.error("buildRequestInfo err", e);
        }
        return httpRequestInfo;
    }

    /**
     * 构建response
     */
    public static ResponseInfo buildResponseInfo(HttpServletResponse resp) {
        long threadId = Thread.currentThread().getId();

        ResponseInfo responseInfo = new ResponseInfo();
        try {
            //获取status
            responseInfo.setStatusCode(resp.getStatus());

            //获取headers
            List<Pair<String, String>> headerList = new LinkedList<Pair<String, String>>();
            Collection<String> headerNames = resp.getHeaderNames();
            for (String headerKey : headerNames) {
                String headerValue = resp.getHeader(headerKey);
                Pair<String, String> pair = new MutablePair<String, String>(headerKey, headerValue);
                headerList.add(pair);
            }
            responseInfo.setHeaders(headerList);

        } catch (Exception e) {
            logger.error("buildResponseInfo err", e);
        }
        return responseInfo;
    }

    public static String getHost(String url) {
        if (StringUtils.isBlank(url) || url.indexOf("//") <= 0) return "";
        try {
            int startIdx = url.indexOf("//") + 2;
            int endIdx = url.substring(startIdx).indexOf("/") + startIdx;
            return url.substring(startIdx, endIdx);
        } catch (Exception e) {
            logger.error("get host err.url={}", url, e);
        }
        return "";
    }

    public static String getTraceUid() {
        try {
            String traceUid = LogContextHolder.get().getTraceId();
            if (StringUtils.isBlank(traceUid)) return "";
            return traceUid;
        } catch (Throwable t) {
            logger.error("get appId err.", t);
        }
        return "";
    }

    public static String getUserUid() {
        try {
            String userUid = LogContextHolder.get().getUserId();
            if (StringUtils.isBlank(userUid)) return "";
            return userUid;
        } catch (Exception e) {
            logger.error("get user uid err.", e);
        }
        return "";
    }

    /**
     * 转换header的value
     */
    private static String transfer(String data) {
        return data.replace("\"", "\\\"");
    }

}
