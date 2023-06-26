package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;

import java.util.LinkedList;
import java.util.List;

public class SpanHandlerHolder {

    private static final List<SpanHandler> handlerList = new LinkedList<SpanHandler>();

    private static final SpanHandler defaultHandler = new DefaultSpanHandler();

    static {
        handlerList.add(new MapperSpanHandler());
        handlerList.add(new Rest1SpanHandler());
        handlerList.add(new Rest2SpanHandler());
        handlerList.add(new SqlSpanHandler());
        handlerList.add(new Dubbo1SpanHandler());
        handlerList.add(new Dubbo2SpanHandler());
        handlerList.add(new MqSpanHandler());
    }

    public static SpanHandler matchHandler(Advice advice, boolean useDefault) {
        for (SpanHandler spanHandler : handlerList) {
            if (spanHandler.hasMatch(advice)) {
                return spanHandler;
            }
        }
        return useDefault ? defaultHandler : null;
    }

    public static SpanHandler matchHandler(Line line, boolean useDefault) {
        for (SpanHandler spanHandler : handlerList) {
            if (spanHandler.hasMatch(line)) {
                return spanHandler;
            }
        }
        return useDefault ? defaultHandler : null;
    }

}
