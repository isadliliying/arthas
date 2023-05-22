package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;

public class MapperSpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String fullClzName = advice.getClazz().getCanonicalName();
        String methodName = advice.getMethod().getName();
        return fullClzName.equals("org.apache.ibatis.binding.MapperMethod") && methodName.equals("execute");
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
