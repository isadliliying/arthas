package com.wingli.arthas.recorder.entity;

import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;

public class ResponseInfo {

    private Integer statusCode = 0;

    private List<Pair<String, String>> headers = new LinkedList<Pair<String, String>>();

    private String body = "";

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
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
}

