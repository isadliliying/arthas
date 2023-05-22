package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.*;

import java.util.LinkedList;

@Name("strace")
@Summary("Display the input/output parameter, return object, and thrown exception of specified method invocation")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  watch org.apache.commons.lang.StringUtils isBlank\n" +
        "  watch org.apache.commons.lang.StringUtils isBlank -t 123abc\n" +
        "  watch org.apache.commons.lang.StringUtils isBlank '{params, target, returnObj, throwExp}' -x 2\n" +
        "  watch *StringUtils isBlank params[0] params[0].length==1\n" +
        "  watch *StringUtils isBlank params '#cost>100'\n" +
        "  watch -f *StringUtils isBlank params\n" +
        "  watch *StringUtils isBlank params[0]\n" +
        "  watch -E -b org\\.apache\\.commons\\.lang\\.StringUtils isBlank params[0]\n" +
        "  watch javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        "  watch OuterClass$InnerClass\n" +
        Constants.WIKI + Constants.WIKI_HOME + "watch")
public class STraceCommand extends EnhancerCommand {

    private String transactionClassPattern;
    private String transactionMethodPattern;

    private String spanClassPattern;
    private String spanMethodPattern;

    private String conditionExpress;
    private String traceUid;
    private String userUid;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 10;

    /**
     * 主 command 是 transaction 的
     */
    private boolean isForTransaction = true;

    /**
     * 是否在 entrance 执行完成之后匹配
     */
    private boolean matchEntranceInFinally = false;

    @Argument(index = 0, argName = "transaction-class-pattern")
    public void setClassPattern(String classPattern) {
        this.transactionClassPattern = classPattern;
    }

    @Argument(index = 1, argName = "transaction-method-pattern")
    public void setMethodPattern(String methodPattern) {
        this.transactionMethodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "condition-express", required = false)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "sc", longName = "spanClass")
    public void setSpanClassPattern(String spanClassPattern) {
        this.spanClassPattern = spanClassPattern;
    }

    @Option(shortName = "sm", longName = "spanMethod")
    public void setSpanMethodPattern(String spanMethodPattern) {
        this.spanMethodPattern = spanMethodPattern;
    }


    @Option(shortName = "f", longName = "finally", flag = true)
    public void setMatchEntranceInFinally(boolean matchEntranceInFinally) {
        this.matchEntranceInFinally = matchEntranceInFinally;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (10 * 1024 * 1024 by default)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default), the max value is " + ObjectView.MAX_DEEP)
    public void setExpand(Integer expand) {
        this.expand = expand;
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

    @Option(shortName = "t", longName = "traceUid")
    @Description("在原条件上增加traceUid条件，相当于加了 @com.seewo.honeycomb.log.LogContextHolder@get().traceId.equals(\"13432432\")")
    public void setTraceUid(String traceUid) {
        this.traceUid = traceUid;
    }

    @Option(shortName = "u", longName = "userUid")
    @Description("在原条件上增加userUid条件，相当于加了 @com.seewo.honeycomb.log.LogContextHolder@get().userId.equals(\"13432432\")")
    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            String pattern = isForTransaction ? getTransactionClassPattern() : getSpanClassPattern();
            classNameMatcher = SearchUtils.classNameMatcher(pattern, isRegEx());
        }
        return classNameMatcher;
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
        if (methodNameMatcher == null) {
            String pattern = isForTransaction ? getTransactionMethodPattern() : getSpanMethodPattern();
            methodNameMatcher = SearchUtils.classNameMatcher(pattern, isRegEx());
        }
        return methodNameMatcher;
    }


    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new STraceAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    public void process(CommandProcess process) {
        //避免循环
        if (isForTransaction) {

            //附属 command 是 span 的
            STraceCommand sTraceCommand = new STraceCommand();
            sTraceCommand.isForTransaction = false;
            sTraceCommand.spanClassPattern = "com.mysql.cj.jdbc.ClientPreparedStatement|org.apache.ibatis.binding.MapperMethod";
            sTraceCommand.spanMethodPattern = "execute";
            sTraceCommand.isRegEx = true;
            sTraceCommand.expand = this.expand;
            sTraceCommand.sizeLimit = this.sizeLimit;
            sTraceCommand.numberOfLimit = this.numberOfLimit;
            sTraceCommand.maxNumOfMatchedClass = 50;

            //先增强 span
            sTraceCommand.process(process);


            //附属 command 是 span 的
            STraceCommand sTraceCommandForRest = new STraceCommand();
            sTraceCommandForRest.isForTransaction = false;
            sTraceCommandForRest.spanClassPattern = "org.springframework.http.client.ClientHttpRequest|org.springframework.web.client.RestTemplate|com.alibaba.dubbo.rpc.protocol.dubbo.DubboInvoker";
            sTraceCommandForRest.spanMethodPattern = "execute|doExecute|doInvoke";
            sTraceCommandForRest.isRegEx = true;
            sTraceCommandForRest.expand = this.expand;
            sTraceCommandForRest.sizeLimit = this.sizeLimit;
            sTraceCommandForRest.numberOfLimit = this.numberOfLimit;
            sTraceCommandForRest.maxNumOfMatchedClass = 50;

            //先增强 span
            sTraceCommandForRest.process(process);

            //附属 command 是 span 的
            STraceCommand sTraceCommandForMq = new STraceCommand();
            sTraceCommandForMq.isForTransaction = false;
            sTraceCommandForMq.spanClassPattern = "com.seewo.framework.mq.client.ProducerTemplate|com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler";
            sTraceCommandForMq.spanMethodPattern = "sendMsg|sendMsgOrderly|invoke";
            sTraceCommandForMq.isRegEx = true;
            sTraceCommandForMq.expand = this.expand;
            sTraceCommandForMq.sizeLimit = this.sizeLimit;
            sTraceCommandForMq.numberOfLimit = this.numberOfLimit;
            sTraceCommandForMq.maxNumOfMatchedClass = 50;

            //先增强 span
            sTraceCommandForMq.process(process);
        }

        super.process(process);

        if (isForTransaction){
            process.write("files will write in " + LogUtil.straceDir() + "\n");
        }
    }

    public String getConditionExpress() {
        LinkedList<String> conditionList = new LinkedList<String>();
        if (!StringUtils.isBlank(traceUid)) {
            conditionList.add("@com.seewo.honeycomb.log.LogContextHolder@get().traceId.equals(\"" + traceUid + "\")");
        }
        if (!StringUtils.isBlank(userUid)) {
            conditionList.add("@com.seewo.honeycomb.log.LogContextHolder@get().userId.equals(\"" + userUid + "\")");
        }
        if (!StringUtils.isBlank(conditionExpress)) {
            conditionList.add(conditionExpress);
        }
        return StringUtils.join(conditionList.toArray(), "&&");
    }

    public Integer getExpand() {
        return expand;
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

    public boolean isForTransaction() {
        return isForTransaction;
    }

    public String getTransactionClassPattern() {
        return transactionClassPattern;
    }

    public String getTransactionMethodPattern() {
        return transactionMethodPattern;
    }

    public String getSpanClassPattern() {
        return spanClassPattern;
    }

    public String getSpanMethodPattern() {
        return spanMethodPattern;
    }

    public boolean isMatchEntranceInFinally() {
        return matchEntranceInFinally;
    }
}
