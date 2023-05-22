package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;

public class SqlSpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String fullClzName = advice.getClazz().getCanonicalName();
        String methodName = advice.getMethod().getName();
        return fullClzName.equals("com.mysql.cj.jdbc.ClientPreparedStatement") && methodName.equals("execute");
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#{\"sql\":target.asSql()}";
    }

}
