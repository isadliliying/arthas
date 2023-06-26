package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;

import java.util.Map;

public class Dubbo1SpanHandler extends AbstractSpanHandler {

    @Override
    public boolean hasMatch(Advice advice) {
        String methodName = advice.getMethod().getName();
        String clazzName = advice.getClazz().getName();
        return "doInvoke".equals(methodName)
                && advice.getParams().length == 1
                && "com.alibaba.dubbo.rpc.protocol.dubbo.DubboInvoker".equals(clazzName);
    }

    @Override
    public boolean hasMatch(Line line) {
        return line.getMark().equals("DubboInvoker#doInvoke");
    }

    @Override
    public Pair<String, String> getEnterMark(Line line) {
        try {
            String targetAddress = line.getObject().getString("targetAddress");
            return Pair.make("[dubbo-remote]", "[" + targetAddress + "]");
        } catch (Exception e) {
            return Pair.make("[dubbo-remote]", "unknown");
        }
    }

    @Override
    public String enterOgnlExpress(Advice advice) {
        return "#apiClassName=target.getInterface().getCanonicalName(),#apiMethodName=params[0].getMethodName(),#apiName=#apiClassName+\"#\"+#apiMethodName,#args=params[0].getArguments(),#attachments=params[0].getAttachments(),#targetHost=target.getUrl().getHost(),#targetPost=target.getUrl().getPort(),#targetAddress=#targetHost+\":\"+#targetPost,#{\"api\":#apiName,\"attachments\":#attachments,\"attachments\":#attachments,\"targetAddress\":#targetAddress}";
    }

    @Override
    public String exitOgnlExpress(Advice advice) {
        return "#result=returnObj.result,#exception=returnObj.exception,#attachments=returnObj.attachments,#{\"exception\":#exception,\"attachments\":#attachments}";
    }

}
