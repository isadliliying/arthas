package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.util.ClassUtils;

public class Rest1SpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String methodName = advice.getMethod().getName();
        return "execute".equals(methodName)
                && advice.getParams().length == 0
                && ClassUtils.hasImplInterface(advice.getClazz(), "org.springframework.http.client.ClientHttpRequest");
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#method=target.getMethod().name,#url=target.getURI().toString(),#reqHeaders=target.getHeaders(),#{\"method\":#method,\"url\":#url,\"headers\":#reqHeaders}";
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        return "#status=returnObj.getRawStatusCode(),#headers=returnObj.getHeaders(),#{\"status\":#status,\"headers\":#headers}";
    }

}
