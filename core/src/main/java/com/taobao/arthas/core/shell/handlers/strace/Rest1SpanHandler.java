package com.taobao.arthas.core.shell.handlers.strace;

import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;
import com.taobao.arthas.core.util.ClassUtils;

import java.util.Map;

public class Rest1SpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String methodName = advice.getMethod().getName();
        return "execute".equals(methodName)
                && advice.getParams().length == 0
                && ClassUtils.hasImplInterface(advice.getClazz(), "org.springframework.http.client.ClientHttpRequest");
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        try {
            String url = line.getObject().getString("url");
            int idx = url.indexOf("?");
            if (idx > 0) {
                url = url.substring(0, idx);
            }
            return Pair.make("[url]", "[" + url + "]");
        } catch (Exception e) {
            return Pair.make("[url]", "unknown");
        }
    }


    @Override
    public boolean hasMatch(Line line) {
        return line.getMark().endsWith("ClientHttpRequest#execute");
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
