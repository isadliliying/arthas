package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.advisor.*;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.RestModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadLocalRest;

import java.util.*;

class RestAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RestAdviceListener.class);
    private final ThreadLocalRest threadLocalRest = new ThreadLocalRest();
    private RestCommand command;
    private CommandProcess process;

    public RestAdviceListener(RestCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    private boolean isFinish() {
        return !command.isException() && !command.isSuccess();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        if ("execute".equals(method.getName()) && args.length == 0 && ClassUtils.hasImplInterface(clazz, "org.springframework.http.client.ClientHttpRequest")) {
            recordRequest(Advice.newForBefore(loader, clazz, method, target, args));
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);

        if ("execute".equals(method.getName()) && args.length == 0 && ClassUtils.hasImplInterface(clazz, "org.springframework.http.client.ClientHttpRequest")) {
            recordRequest(advice);
        }

        if ("doExecute".equals(method.getName()) && "org.springframework.web.client.RestTemplate".equals(clazz.getName())) {
            if (command.isSuccess()) {
                watching(advice);
            } else {
                finishing(advice);
            }
        }

    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        if ("doExecute".equals(method.getName()) && "org.springframework.web.client.RestTemplate".equals(clazz.getName())) {
            Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
            if (command.isException()) {
                watching(advice);
            } else {
                finishing(advice);
            }
        }

    }

    private void finishing(Advice advice) {
        if (!command.isException() && !command.isSuccess()) {
            watching(advice);
        }
    }

    private void watching(Advice advice) {
        try {
            String dataExpress = "#hasRequestBody=params[2] instanceof org.springframework.web.client.RestTemplate$HttpEntityRequestCallback,#requestBody=#hasRequestBody?params[2].requestEntity.body:null,#hasReturnEntity=returnObj instanceof org.springframework.http.ResponseEntity,#responseStatus=#hasReturnEntity?returnObj.status:null,#responseHeaders=#hasReturnEntity?returnObj.headers:null,#responseBody=#hasReturnEntity?returnObj.body:null,#rawReturnObj=returnObj,{#hasRequestBody,#requestBody,#hasReturnEntity,#responseStatus,#responseHeaders,#responseBody,#rawReturnObj}";
            ArrayList<Object> objectArrayList = (ArrayList<Object>) ExpressFactory.threadLocalExpress(advice).get(dataExpress);
            Boolean hasRequestBody = (Boolean) objectArrayList.get(0);
            Object requestBody = objectArrayList.get(1);
            Boolean hasReturnEntity = (Boolean) objectArrayList.get(2);
            //Integer responseStatus = (Integer) objectArrayList.get(3);
            //Object responseHeadersObj = objectArrayList.get(4);
            Object responseBody = objectArrayList.get(5);
            Object rawReturnObj = objectArrayList.get(6);

            String requestBodyAsJstr = JSON.toJSONString(requestBody);
            String rawReturnAsJstr = JSON.toJSONString(rawReturnObj);

            RestAdvice restAdvice = threadLocalRest.loadRestAdvice();

            RestAdvice.Req req = restAdvice.getReq();
            req.setBody(requestBody);
            req.setBodyAsJstr(requestBodyAsJstr);
            req.setHasBody(hasRequestBody);

            RestAdvice.Resp resp = restAdvice.getResp();
            resp.setHasBody(hasReturnEntity);
            resp.setBody(responseBody);
            resp.setRawReturnObj(rawReturnObj);
            resp.setRawReturnStr(rawReturnAsJstr);
            resp.setEndTime(System.currentTimeMillis());

            String conditionExpress = command.getConditionExpress();
            if (StringUtils.isEmpty(conditionExpress)
                    || ExpressFactory.threadLocalExpress(restAdvice).is(conditionExpress)) {

                long cost = resp.getEndTime() - req.getStartTime();
                RestModel restModel = new RestModel();
                restModel.setTs(new Date());
                restModel.setCostInMs(cost);
                restModel.setSizeLimit(command.getSizeLimit());
                restModel.setMethod(req.getMethod());
                restModel.setUri(req.getUri());
                restModel.setRequestHeaders(req.getHeaders());
                restModel.setRequestBody(new ObjectVO(req.getBody(), command.getExpand()));
                restModel.setStatus(resp.getStatus());
                restModel.setResponseHeaders(resp.getHeaders());
                restModel.setResponseBody(new ObjectVO(resp.getBody(), command.getExpand()));

                process.appendResult(restModel);
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    abortProcess(process, command.getNumberOfLimit());
                }

            }
        } catch (Throwable e) {
            logger.warn("watch failed.", e);
            process.end(-1, "watch failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }


    private void recordRequest(Advice advice) {
        try {
            if (advice.isBefore()) {
                String dataExpress = "#method=target.getMethod().name,#url=target.getURI().toString(),#reqHeaders=target.getHeaders(),{#method,#url,#reqHeaders}";
                ArrayList<Object> objectArrayList = (ArrayList<Object>) ExpressFactory.threadLocalExpress(advice).get(dataExpress);
                String method = (String) objectArrayList.get(0);
                String uri = (String) objectArrayList.get(1);
                Object headerObj = objectArrayList.get(2);
                Map<String, List<String>> headers = parseHttpHeaders(headerObj);

                RestAdvice restAdvice = threadLocalRest.loadRestAdvice();
                RestAdvice.Req req = restAdvice.getReq();
                req.setMethod(method);
                req.setUri(uri);
                req.setHeaders(headers);
                req.setStartTime(System.currentTimeMillis());
            }

            if (advice.isAfterReturning()) {
                String dataExpress = "#status=returnObj.getRawStatusCode(),#headers=returnObj.getHeaders(),#arr={#status,#headers}";
                ArrayList<Object> objectArrayList = (ArrayList<Object>) ExpressFactory.threadLocalExpress(advice).get(dataExpress);
                Integer responseStatus = (Integer) objectArrayList.get(0);
                Object responseHeadersObj = objectArrayList.get(1);
                Map<String, List<String>> headers = parseHttpHeaders(responseHeadersObj);

                RestAdvice restAdvice = threadLocalRest.loadRestAdvice();
                RestAdvice.Resp resp = restAdvice.getResp();
                resp.setStatus(responseStatus);
                resp.setHeaders(headers);
            }
        } catch (Throwable e) {
            logger.warn("rest: record request failed.", e);
            process.end(-1, "rest: record request failed " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }

    }

    /**
     * 转换header
     */
    public static Map<String, List<String>> parseHttpHeaders(Object headerObj) {
        JSONObject headerJSONObj = (JSONObject) JSONObject.toJSON(headerObj);
        Map<String, List<String>> headers = new HashMap<String, List<String>>(headerJSONObj.size());
        for (Map.Entry<String, Object> stringObjectEntry : headerJSONObj.entrySet()) {
            List<String> lines = ((JSONArray) stringObjectEntry.getValue()).toJavaList(String.class);
            headers.put(stringObjectEntry.getKey(), lines);
        }
        return headers;
    }

}
