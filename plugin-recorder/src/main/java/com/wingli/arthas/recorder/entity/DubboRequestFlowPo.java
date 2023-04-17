package com.wingli.arthas.recorder.entity;

public class DubboRequestFlowPo {

    /**
     * 接口
     */
    private String dubbo_interface = null;
    /**
     * 方法
     */
    private String dubbo_method = null;
    /**
     * 请求参数
     */
    private String dubbo_request_args = null;
    /**
     * 请求的attachments
     */
    private String dubbo_request_attachments = null;

    public String getDubbo_interface() {
        return dubbo_interface;
    }

    public void setDubbo_interface(String dubbo_interface) {
        this.dubbo_interface = dubbo_interface;
    }

    public String getDubbo_method() {
        return dubbo_method;
    }

    public void setDubbo_method(String dubbo_method) {
        this.dubbo_method = dubbo_method;
    }

    public String getDubbo_request_args() {
        return dubbo_request_args;
    }

    public void setDubbo_request_args(String dubbo_request_args) {
        this.dubbo_request_args = dubbo_request_args;
    }

    public String getDubbo_request_attachments() {
        return dubbo_request_attachments;
    }

    public void setDubbo_request_attachments(String dubbo_request_attachments) {
        this.dubbo_request_attachments = dubbo_request_attachments;
    }

}
