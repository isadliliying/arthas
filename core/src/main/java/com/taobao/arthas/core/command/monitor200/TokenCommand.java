package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.*;

import java.util.Arrays;

@Name("token")
@Summary("replace header !")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  token 0e6eae5449a4443ffa9c141dbf4b8e900 0b90a5743b70d43d383712735787f0ebd\n" +
        "  token -H x-minder-token 0e6eae5449a4443ffa9c141dbf4b8e900 0b90a5743b70d43d383712735787f0ebd\n" +
        Constants.WIKI + Constants.WIKI_HOME + "token")
public class TokenCommand extends EnhancerCommand {

    private String oldValue;
    private String newValue;
    private String header;


    @Argument(index = 0, argName = "old-header-value")
    @Description("旧值")
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    @Argument(index = 1, argName = "new-header-value")
    @Description("新值")
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Option(shortName = "H", longName = "header")
    @DefaultValue("x-auth-token")
    @Description("header的key，默认为 x-auth-token")
    public void setHeader(String header) {
        this.header = header;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        return SearchUtils.classNameMatcher("org.apache.coyote.http11.Http11InputBuffer", false);
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        return null;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        return SearchUtils.classNameMatcher("parseHeaders", false);
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {

        WatchCommand watchCommand = new WatchCommand();
        watchCommand.setClassPattern("org.apache.coyote.http11.Http11InputBuffer");
        watchCommand.setMethodPattern("parseHeaders");
        String express =
                "#headerName=\"" + header + "\",#oldHeaderValue=\"" + oldValue + "\",#newHeaderValue=\"" + newValue + "\",#headers=target.headers," + "" +
                        "#uri=target.request.uriMB.toString(),#methodName=target.request.methodMB.toString()," +
                        "#match=#headers.getHeader(#headerName).equals(#oldHeaderValue)," +
                        "#match ? #headers.setValue(#headerName).setString(#newHeaderValue):false," +
                        "#match ? #methodName+\" \"+#uri+\": replaced header[\"+#headerName+\"] \"+#oldHeaderValue+\" to \"+ #newHeaderValue :false";
        watchCommand.setExpress(express);
        watchCommand.setConditionExpress("#headerName=\"" + header + "\",#oldHeaderValue=\"" + oldValue + "\",#curHeaderValue=target.headers.getHeader(#headerName),#oldHeaderValue.equals(#curHeaderValue)");
        watchCommand.setBefore(false);
        watchCommand.setFinish(true);
        watchCommand.setException(false);
        watchCommand.setSuccess(false);
        watchCommand.setExpand(1);
        watchCommand.setSizeLimit(10 * 1024 * 1024);
        watchCommand.setRegEx(false);
        watchCommand.setNumberOfLimit(Integer.MAX_VALUE);

        return new WatchAdviceListener(watchCommand, process, GlobalOptions.verbose || this.verbose);
    }

}
