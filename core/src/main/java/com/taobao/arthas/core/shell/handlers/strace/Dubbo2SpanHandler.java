package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;

public class Dubbo2SpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String methodName = advice.getMethod().getName();
        String clazzName = advice.getClazz().getName();
        return "invoke".equals(methodName)
                && advice.getParams().length == 3
                && "com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler".equals(clazzName);
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#api=params[1].getDeclaringClass().getName()+\"#\"+params[1].getName(),#args=params[2],#{\"api\":#api,\"args\":#args}";
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        return "#{\"returnObj\":returnObj}";
    }

}
