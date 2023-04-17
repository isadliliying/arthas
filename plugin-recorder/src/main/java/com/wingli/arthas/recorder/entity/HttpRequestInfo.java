package com.wingli.arthas.recorder.entity;

import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;

public class HttpRequestInfo {

    private String fullUrl = "";

    private String urlWithoutQuery = "";

    private String method = "";

    private List<Pair<String, String>> headers = new LinkedList<Pair<String, String>>();

    private String body = "";

    private String encode = "";

    public String getEncode() {
        return encode;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Pair<String, String>> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Pair<String, String>> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUrlWithoutQuery() {
        return urlWithoutQuery;
    }

    public void setUrlWithoutQuery(String urlWithoutQuery) {
        this.urlWithoutQuery = urlWithoutQuery;
    }
}

