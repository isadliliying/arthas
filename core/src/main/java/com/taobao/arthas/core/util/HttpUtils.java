package com.taobao.arthas.core.util;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.model.RestModel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author gongdewei 2020/3/31
 */
public class HttpUtils {

    private static final String CURL_FORMAT_HEADER = "-H \"%1$s:%2$s\"";
    private static final String CURL_FORMAT_METHOD = "-X %1$s";
    private static final String CURL_FORMAT_BODY = "-d \"%1$s\"";
    private static final String CURL_FORMAT_URL = "\"%1$s\"";

    private static final String DUBBO_FORMAT_INVOKE = "invoke %s.%s(%s)";

    private static final String DUBBO_FORMAT_SCRIPT = "{ echo '%s'; sleep 3;} | telnet %s %s";

    private static String transfer(String data) {
        return data.replace("\"", "\\\"");
    }

    public static String buildCUrl(RestModel restModel) {
        try {
            List<String> parts = new ArrayList<String>();
            parts.add("curl");

            //method
            parts.add(String.format(CURL_FORMAT_METHOD, restModel.getMethod().toUpperCase()));

            String realUrl = restModel.getUri();

            //header
            for (Map.Entry<String, List<String>> headerEntry : restModel.getRequestHeaders().entrySet()) {
                String headerValue = transfer(StringUtils.join(headerEntry.getValue().toArray(new String[0]), ";"));
                //移除accept-encoding，因为可能会返回gzip压缩
                if ("accept-encoding".equalsIgnoreCase(headerEntry.getKey())) {
                    parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-origin-accept-encoding"), headerValue));
                    continue;
                }
                //移除content-type，不然修改内容的时候，很容易忽略所以还不如直接改掉好了
                if ("content-length".equalsIgnoreCase(headerEntry.getKey())) {
                    parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-origin-content-length"), headerValue));
                    continue;
                }
                parts.add(String.format(CURL_FORMAT_HEADER, transfer(headerEntry.getKey()), headerValue));
            }

            //特殊标记
            parts.add(String.format(CURL_FORMAT_HEADER, transfer("x-from-curl"), "true"));

            //body
            if (restModel.getRequestBody() != null){
                Object bodyObj = restModel.getRequestBody().getObject();
                if (bodyObj!=null){
                    String bodyStr = JSON.toJSONString(bodyObj);
                    parts.add(String.format(CURL_FORMAT_BODY, transfer(bodyStr)));
                }
            }

            //url
            parts.add(String.format(CURL_FORMAT_URL, transfer(realUrl)));

            return StringUtils.join(parts.toArray(), " \\\r\n");
        } catch (Exception e) {
            //ignore
        }
        return "";
    }


    /**
     * Get cookie value by name
     *
     * @param cookies    request cookies
     * @param cookieName the cookie name
     */
    public static String getCookieValue(Set<Cookie> cookies, String cookieName) {
        for (Cookie cookie : cookies) {
            if (cookie.name().equals(cookieName)) {
                return cookie.value();
            }
        }
        return null;
    }

    /**
     * @param response
     * @param name
     * @param value
     */
    public static void setCookie(DefaultFullHttpResponse response, String name, String value) {
        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(name, value));
    }

    /**
     * Create http response with status code and content
     *
     * @param request request
     * @param status  response status code
     * @param content response content
     */
    public static DefaultHttpResponse createResponse(FullHttpRequest request, HttpResponseStatus status, String content) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        try {
            response.content().writeBytes(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        return response;
    }

    public static HttpResponse createRedirectResponse(FullHttpRequest request, String url) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, url);
        return response;
    }
}
