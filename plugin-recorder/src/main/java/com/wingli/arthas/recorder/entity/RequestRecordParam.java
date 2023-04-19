package com.wingli.arthas.recorder.entity;

public class RequestRecordParam {
    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 环境
     */
    private String env;

    /**
     * appId
     */
    private String appId;

    /**
     * ip
     */
    private String ipAddr;

    /**
     * 请求类型
     */
    private String requestType;

    /**
     * 请求方式
     */
    private String httpRequestMethod;

    /**
     * 请求url
     */
    private String httpRequestUrl;

    /**
     * 请求body
     */
    private String httpRequestBody;

    /**
     * 请求header
     */
    private String httpRequestHeaders;

    /**
     * 请求token
     */
    private String httpTokenUid;

    /**
     * 请求traceUid
     */
    private String httpTraceUid;

    /**
     * 请求userUid
     */
    private String httpUserUid;

    /**
     * interface
     */
    private String dubboInterface;

    /**
     * method
     */
    private String dubboMethod;

    /**
     * 参数
     */
    private String dubboRequestArgs;

    /**
     * 上下文
     */
    private String dubboRequestAttachments;


    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getHttpRequestMethod() {
        return httpRequestMethod;
    }

    public void setHttpRequestMethod(String httpRequestMethod) {
        this.httpRequestMethod = httpRequestMethod;
    }

    public String getHttpRequestUrl() {
        return httpRequestUrl;
    }

    public void setHttpRequestUrl(String httpRequestUrl) {
        this.httpRequestUrl = httpRequestUrl;
    }

    public String getHttpRequestBody() {
        return httpRequestBody;
    }

    public void setHttpRequestBody(String httpRequestBody) {
        this.httpRequestBody = httpRequestBody;
    }

    public String getHttpRequestHeaders() {
        return httpRequestHeaders;
    }

    public void setHttpRequestHeaders(String httpRequestHeaders) {
        this.httpRequestHeaders = httpRequestHeaders;
    }

    public String getHttpTokenUid() {
        return httpTokenUid;
    }

    public void setHttpTokenUid(String httpTokenUid) {
        this.httpTokenUid = httpTokenUid;
    }

    public String getHttpTraceUid() {
        return httpTraceUid;
    }

    public void setHttpTraceUid(String httpTraceUid) {
        this.httpTraceUid = httpTraceUid;
    }

    public String getHttpUserUid() {
        return httpUserUid;
    }

    public void setHttpUserUid(String httpUserUid) {
        this.httpUserUid = httpUserUid;
    }

    public String getDubboInterface() {
        return dubboInterface;
    }

    public void setDubboInterface(String dubboInterface) {
        this.dubboInterface = dubboInterface;
    }

    public String getDubboMethod() {
        return dubboMethod;
    }

    public void setDubboMethod(String dubboMethod) {
        this.dubboMethod = dubboMethod;
    }

    public String getDubboRequestArgs() {
        return dubboRequestArgs;
    }

    public void setDubboRequestArgs(String dubboRequestArgs) {
        this.dubboRequestArgs = dubboRequestArgs;
    }

    public String getDubboRequestAttachments() {
        return dubboRequestAttachments;
    }

    public void setDubboRequestAttachments(String dubboRequestAttachments) {
        this.dubboRequestAttachments = dubboRequestAttachments;
    }
}
