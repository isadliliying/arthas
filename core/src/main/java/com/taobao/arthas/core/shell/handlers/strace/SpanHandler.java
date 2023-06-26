package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.Line;
import com.taobao.arthas.core.util.StraceUtils;

public interface SpanHandler {

    boolean hasMatch(Advice advice);

    boolean hasMatch(Line line);

    Pair<String,String> getEnterMark(Line line);

    String enterOgnlExpress(Advice advice);

    String exitOgnlExpress(Advice advice);

    Object enterOgnlObj(Advice advice);

    Object exitOgnlObj(Advice advice);

}
