package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.*;
import org.benf.cfr.reader.util.collections.CollectionUtils;

import java.util.Arrays;
import java.util.LinkedList;

public class RecorderWatcherCommand extends EnhancerCommand {

    @Override
    protected Matcher getClassNameMatcher() {
        return SearchUtils.classNameMatcher("com.wingli.arthas.recorder.RequestRecordHelper", false);
    }

    /**
     * 排除类名匹配
     */
    @Override
    protected Matcher getClassNameExcludeMatcher() {
        return null;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        return SearchUtils.classNameMatcher("stat", false);
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new RecorderWatcherAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

}
