package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;

import java.util.Map;

public class SqlSpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String fullClzName = advice.getClazz().getCanonicalName();
        String methodName = advice.getMethod().getName();
        return fullClzName.equals("com.mysql.cj.jdbc.ClientPreparedStatement") && methodName.equals("execute");
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        try {
            String sql = line.getObject().getString("sql").replace("\n"," ");
            if (sql.length() > 50){
                sql = sql.substring(0, 49);
            }
            return Pair.make("[sql]", "[" + sql + "]");
        } catch (Exception e) {
            return Pair.make("[sql]", "unknown");
        }
    }

    @Override
    public boolean hasMatch(Line line) {
        return line.getMark().equals("ClientPreparedStatement#execute");
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#{\"sql\":target.asSql()}";
    }

}
