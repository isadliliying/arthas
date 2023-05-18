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

@Name("rest")
@Summary("generate curl command when using RestTemplate!")
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
        Constants.WIKI + Constants.WIKI_HOME + "rest")
public class RestCommand extends EnhancerCommand {

    private String conditionExpress;
    private boolean isException = false;
    private boolean isSuccess = false;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private int numberOfLimit = 100;

    @Argument(index = 3, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "e", longName = "exception", flag = true)
    @Description("Watch after throw exception")
    public void setException(boolean exception) {
        isException = exception;
    }

    @Option(shortName = "s", longName = "success", flag = true)
    @Description("Watch after successful invocation")
    public void setSuccess(boolean success) {
        isSuccess = success;
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

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public boolean isException() {
        return isException;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Integer getExpand() {
        return expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        return SearchUtils.classNameMatcher("org.springframework.http.client.ClientHttpRequest|org.springframework.web.client.RestTemplate", true);
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        return null;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        return SearchUtils.classNameMatcher("execute|doExecute", true);
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new RestAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

}
