package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;

import java.util.Map;

public class MapperSpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String fullClzName = advice.getClazz().getCanonicalName();
        String methodName = advice.getMethod().getName();
        return fullClzName.equals("org.apache.ibatis.binding.MapperMethod") && methodName.equals("execute");
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        try {
            String mapper = line.getObject().getString("mapper");
            return Pair.make("[mapper]", "[" + mapper + "]");
        } catch (Exception e) {
            return Pair.make("[mapper]", "unknown");
        }
    }

    @Override
    public boolean hasMatch(Line line) {
        return line.getMark().equals("MapperMethod#execute");
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#{\"arg\":params[1],\"mapper\":target.command.name}";
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        return "#{\"mapperResult\":returnObj}";
    }


}
