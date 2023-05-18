package com.taobao.arthas.core.command.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class RestModel extends ResultModel {

    private Date ts;
    private Long costInMs;

    private Integer sizeLimit;

    private String method;

    private String uri;

    private Map<String, List<String>> requestHeaders;

    private ObjectVO requestBody;

    private Integer status;

    private Map<String, List<String>> responseHeaders;

    private ObjectVO responseBody;


    public RestModel() {
    }

    @Override
    public String getType() {
        return "rest";
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    public Long getCostInMs() {
        return costInMs;
    }

    public void setCostInMs(Long costInMs) {
        this.costInMs = costInMs;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, List<String>> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public ObjectVO getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(ObjectVO requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public ObjectVO getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(ObjectVO responseBody) {
        this.responseBody = responseBody;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }
}
