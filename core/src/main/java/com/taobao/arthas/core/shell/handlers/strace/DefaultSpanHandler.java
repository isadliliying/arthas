package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.util.ClassUtils;

import java.util.HashMap;

public class DefaultSpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        return true;
    }

    @Override
    public Object enterOgnlObj(Advice advice) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("params", advice.getParams());
        return map;
    }

    @Override
    public Object exitOgnlObj(Advice advice) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("returnObj", advice.getReturnObj());
        return map;
    }
}
