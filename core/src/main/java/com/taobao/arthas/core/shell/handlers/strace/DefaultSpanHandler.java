package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;
import com.taobao.arthas.core.util.ClassUtils;

import java.util.HashMap;

public class DefaultSpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        return true;
    }

    @Override
    public boolean hasMatch(Line line) {
        return false;
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        int idx = line.getMark().indexOf("#");
        if (idx < 0) {
            return Pair.make(line.getMark(), "");
        } else {
            String first = line.getMark().substring(0, idx);
            String second = line.getMark().substring(idx + 1);
            return Pair.make(first, second);
        }
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
