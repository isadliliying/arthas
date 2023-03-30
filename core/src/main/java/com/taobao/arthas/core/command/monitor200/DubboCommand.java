package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.*;

import java.util.Arrays;

@Name("dubbo")
@Summary("generate dubbo telnet invoke command!")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  dubbo com.seewo.api.XxxService query\n" +
        "  dubbo -b com.seewo.api.XxxService query\n" +
        "  dubbo *StringUtils isBlank 'params[0].length==1'\n" +
        Constants.WIKI + Constants.WIKI_HOME + "dubbo")
public class DubboCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    private boolean isBefore = false;
    private boolean isFinish = false;
    private boolean isException = false;
    private boolean isSuccess = false;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;

    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to watch")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to watch")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "b", longName = "before", flag = true)
    @Description("generate before invocation")
    public void setBefore(boolean before) {
        isBefore = before;
    }

    @Option(shortName = "f", longName = "finish", flag = true)
    @Description("generate after invocation, enable by default")
    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    @Option(shortName = "e", longName = "exception", flag = true)
    @Description("generate after throw exception")
    public void setException(boolean exception) {
        isException = exception;
    }

    @Option(shortName = "s", longName = "success", flag = true)
    @Description("generate after successful invocation")
    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (10 * 1024 * 1024 by default)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public boolean isException() {
        return isException;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        WatchCommand watchCommand = new WatchCommand();
        watchCommand.setClassPattern(this.getClassPattern());
        watchCommand.setMethodPattern(this.getMethodPattern());
        String express =
                "@com.alibaba.dubbo.common.utils.PojoUtils@generalize(1),\n"+
                "#clzName=clazz.getInterfaces().{^ !#this.toString().contains(\"com.alibaba\")}[0].getName(),\n" +
                "#medName=method.methodName,\n" +
                "#pams=@com.alibaba.fastjson.JSON@toJSONString(@com.alibaba.dubbo.common.utils.PojoUtils@generalize(params).{#this}).replaceAll(\"\\\\u005B(.*)]\",\"$1\").replace(\"'\",\"'\\\"'\\\"'\"),\n" +
                "#invokeCmd=@java.lang.String@format(\"invoke %s.%s(%s)\",#clzName,#medName,#pams),\n" +
                "#url=target.handler.invoker.directory.urlInvokerMap.values().toArray()[0].providerUrl,#providerIp=#url.host,#providerPort=#url.port,\n" +
                "@java.lang.String@format(\"{ echo '%s';sleep 3;} | telnet %s %s\",#invokeCmd,#providerIp,#providerPort)\n";
        watchCommand.setExpress(express);
        watchCommand.setConditionExpress(this.getConditionExpress());
        watchCommand.setBefore(this.isBefore());
        watchCommand.setFinish(this.isFinish());
        watchCommand.setException(this.isException());
        watchCommand.setSuccess(this.isSuccess());
        watchCommand.setExpand(1);
        watchCommand.setSizeLimit(this.getSizeLimit());
        watchCommand.setRegEx(this.isRegEx());
        watchCommand.setNumberOfLimit(this.getNumberOfLimit());

        return new WatchAdviceListener(watchCommand, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
