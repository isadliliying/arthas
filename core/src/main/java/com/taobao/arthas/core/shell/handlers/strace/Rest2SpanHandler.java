package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;

public class Rest2SpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String methodName = advice.getMethod().getName();
        String className = advice.getClazz().getName();
        return "doExecute".equals(methodName) && "org.springframework.web.client.RestTemplate".equals(className);
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#hasRequestBody=params[2] instanceof org.springframework.web.client.RestTemplate$HttpEntityRequestCallback,#requestBody=#hasRequestBody?params[2].requestEntity.body:null,#{\"requestBody\":#requestBody}";
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        return "#hasReturnEntity=returnObj instanceof org.springframework.http.ResponseEntity,#responseBody=#hasReturnEntity?returnObj.body:null,#useReturnObj=#hasReturnEntity?#responseBody:returnObj,#{\"useReturnObj\":#useReturnObj}";
    }

}
