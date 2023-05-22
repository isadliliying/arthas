package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.express.ExpressFactory;

import java.util.HashMap;
import java.util.Map;

abstract class AbstractSpanHandler implements SpanHandler {

    private static final Map<String,String> defaultValue = new HashMap<String, String>();

    static {
        defaultValue.put("msg","ignored");
    }

    @Override
    public boolean hasMatch(Advice advice) {
        return false;
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return null;
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        return null;
    }

    @Override
    public Object enterOgnlObj(Advice advice) {
        String express = enterOgnlExpress(advice);
        if (express == null) return defaultValue;
        try {
            return ExpressFactory.unpooledExpress(advice.getLoader()).bind(advice).get(express);
        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    @Override
    public Object exitOgnlObj(Advice advice) {
        String express = exitOgnlExpress(advice);
        if (express == null) return defaultValue;
        try {
            return ExpressFactory.unpooledExpress(advice.getLoader()).bind(advice).get(express);
        } catch (Exception e) {
            //ignore
        }
        return null;
    }


}
