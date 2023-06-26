package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;

import java.util.Map;

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
    public boolean hasMatch(Line line) {
        return line.getMark().equals("InvokerInvocationHandler#invoke");
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        try {
            String api = line.getObject().getString("api");
            return Pair.make("[dubbo-api]", "[" + api + "]");
        } catch (Exception e) {
            return Pair.make("[dubbo-api]", "unknown");
        }
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
