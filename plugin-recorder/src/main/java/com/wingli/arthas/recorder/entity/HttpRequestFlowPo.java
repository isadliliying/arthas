package com.wingli.arthas.recorder.entity;

public class HttpRequestFlowPo {

    /**
     * 追踪uid
     */
    private String trace_uid = null;
    /**
     * user
     */
    private String user_uid = null;
    /**
     * token
     */
    private String http_token_uid = null;
    /**
     * 请求方法
     */
    private String http_request_method = null;
    /**
     * url
     */
    private String http_request_url = null;
    /**
     * 请求头
     */
    private String http_request_headers = null;
    /**
     * 请求体
     */
    private String http_request_body = null;

    public String getTrace_uid() {
        return trace_uid;
    }

    public void setTrace_uid(String trace_uid) {
        this.trace_uid = trace_uid;
    }

    public String getUser_uid() {
        return user_uid;
    }

    public void setUser_uid(String user_uid) {
        this.user_uid = user_uid;
    }

    public String getHttp_token_uid() {
        return http_token_uid;
    }

    public void setHttp_token_uid(String http_token_uid) {
        this.http_token_uid = http_token_uid;
    }

    public String getHttp_request_method() {
        return http_request_method;
    }

    public void setHttp_request_method(String http_request_method) {
        this.http_request_method = http_request_method;
    }

    public String getHttp_request_url() {
        return http_request_url;
    }

    public void setHttp_request_url(String http_request_url) {
        this.http_request_url = http_request_url;
    }

    public String getHttp_request_headers() {
        return http_request_headers;
    }

    public void setHttp_request_headers(String http_request_headers) {
        this.http_request_headers = http_request_headers;
    }

    public String getHttp_request_body() {
        return http_request_body;
    }

    public void setHttp_request_body(String http_request_body) {
        this.http_request_body = http_request_body;
    }
}
