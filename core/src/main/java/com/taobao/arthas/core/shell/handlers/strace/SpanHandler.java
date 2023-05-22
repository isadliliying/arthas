package com.taobao.arthas.core.shell.handlers.strace;

import com.taobao.arthas.core.advisor.Advice;

public interface SpanHandler {

    boolean hasMatch(Advice advice);

    String enterOgnlExpress(Advice advice);

    String exitOgnlExpress(Advice advice);

    Object enterOgnlObj(Advice advice);

    Object exitOgnlObj(Advice advice);

}
