package com.taobao.arthas.core.advisor;

import java.util.List;
import java.util.Map;

public class RestAdvice {


    private Req req;

    private Resp resp;

    public Req getReq() {
        return req;
    }

    public void setReq(Req req) {
        this.req = req;
    }

    public Resp getResp() {
        return resp;
    }

    public void setResp(Resp resp) {
        this.resp = resp;
    }

    public static class Req {

        private String method;

        private String uri;

        private Map<String, List<String>> headers;

        private Boolean hasBody;

        private Object body;

        private String bodyAsJstr;

        private Long startTime;

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

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public Boolean getHasBody() {
            return hasBody;
        }

        public void setHasBody(Boolean hasBody) {
            this.hasBody = hasBody;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        public String getBodyAsJstr() {
            return bodyAsJstr;
        }

        public void setBodyAsJstr(String bodyAsJstr) {
            this.bodyAsJstr = bodyAsJstr;
        }

        public Long getStartTime() {
            return startTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }
    }

    public static class Resp {

        private Boolean hasBody;

        private Integer status;

        private Map<String, List<String>> headers;

        private Object body;

        private Object rawReturnObj;

        private Object rawReturnStr;

        private Long endTime;

        public Boolean getHasBody() {
            return hasBody;
        }

        public void setHasBody(Boolean hasBody) {
            this.hasBody = hasBody;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        public Object getRawReturnObj() {
            return rawReturnObj;
        }

        public void setRawReturnObj(Object rawReturnObj) {
            this.rawReturnObj = rawReturnObj;
        }

        public Object getRawReturnStr() {
            return rawReturnStr;
        }

        public void setRawReturnStr(Object rawReturnStr) {
            this.rawReturnStr = rawReturnStr;
        }


        public Long getEndTime() {
            return endTime;
        }

        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }
    }

}
