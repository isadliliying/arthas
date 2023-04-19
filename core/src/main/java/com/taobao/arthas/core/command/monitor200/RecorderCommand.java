package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.basic1000.OptionsCommand;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.EchoModel;
import com.taobao.arthas.core.command.model.RecorderModel;
import com.taobao.arthas.core.command.model.VmToolModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.*;
import com.taobao.middleware.cli.annotations.*;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.List;

@Name("recorder")
@Summary("replace vmtool --action getInstances --className org.springframework.context.ApplicationContext")
@Description(Constants.EXAMPLE
        + "  instances demo.MathGame 'instances[0]'\n"
        + "  instances demo.MathGame 'instances.length'\n"
        + "  instances demo.MathGame demo.MathGame -x 2\n"
        + "  instances demo.MathGame java.lang.String --limit 10\n"
        + "  instances demo.MathGame java.lang.String -t 123abcd\n"
        + Constants.WIKI + Constants.WIKI_HOME + "instances")
//@formatter:on
public class RecorderCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(RecorderCommand.class);

    private volatile boolean isHolder = false;

    private String dubboExpress;

    private String httpExpress;

    private String taskName;

    private int numLimit;

    @Option(shortName = "d", longName = "dubbo")
    @DefaultValue("")
    @Description("dubbo match regex! use qualify name! eg: com.ccc.xxx.bbbApi#method")
    public void setDubboExpress(String dubboExpress) {
        this.dubboExpress = dubboExpress;
    }

    @Option(shortName = "h", longName = "http")
    @DefaultValue("")
    @Description("http match regex! use uri! eg: GET /v1/store/")
    public void setHttpExpress(String dubboExpress) {
        this.httpExpress = dubboExpress;
    }

    @Option(shortName = "n", longName = "limit")
    @DefaultValue("10")
    @Description("记录上限,默认10")
    public void setNumLimit(int numLimit) {
        this.numLimit = numLimit;
    }

    @Option(shortName = "t", longName = "taskName")
    @DefaultValue("undefine")
    @Description("任务名称,如: app-store-dubbo")
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public void process(final CommandProcess process) {
        try {
            process.ignoreEnd();
            process.justShowRecorderView(true);

            final Instrumentation inst = process.session().getInstrumentation();
            process.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    if (isHolder) {
                        String express = "@com.wingli.arthas.recorder.RequestRecordHelper@onShutdown()";
                        runExpress(inst, express);
                    }
                }
            });

            System.setProperty("arthas.recorder.limit", String.valueOf(numLimit));
            process.appendResult(new RecorderModel("recorder: limit " + numLimit));
            System.setProperty("arthas.recorder.http", httpExpress);
            process.appendResult(new RecorderModel("recorder: http " + String.valueOf(httpExpress)));
            System.setProperty("arthas.recorder.dubbo", dubboExpress);
            process.appendResult(new RecorderModel("recorder: dubbo " + String.valueOf(dubboExpress)));
            System.setProperty("arthas.recorder.task.name", taskName);
            process.appendResult(new RecorderModel("recorder: task name " + String.valueOf(taskName)));


            //vmtool --action getInstances --className org.springframework.boot.loader.LaunchedURLClassLoader -express ''
            {
                String jarUrl = ArthasBootstrap.arthasHome() + "/arthas-recorder.jar";
                String newExpress =
                        "#classloader=@java.lang.Thread@currentThread().getContextClassLoader()," +
                                "#url=new java.net.URL(\"jar:file:" + jarUrl + "!/\")," +
                                "classloader.ucp.addURL(#url)";
                runExpress(process.session().getInstrumentation(), newExpress);
                process.appendResult(new RecorderModel("recorder: add " + jarUrl + " to classloader!"));

                boolean isRunning = Boolean.parseBoolean(runExpress(inst, "@com.wingli.arthas.recorder.RequestRecordHelper@isRecording.get()").toString());
                if (isRunning) {
                    throw new IllegalArgumentException("recorder is already running");
                }
                runExpress(process.session().getInstrumentation(), "@com.wingli.arthas.recorder.RequestRecordHelper@onStart()");
                process.appendResult(new RecorderModel("recorder: call recorder on start!"));
                isHolder = true;
            }

            //禁用子类增强，不处理并发
            boolean hasChangeGlobalOptions = false;
            if (!GlobalOptions.isDisableSubClass) {
                GlobalOptions.isDisableSubClass = true;
                hasChangeGlobalOptions = true;
            }

            //watch '@com.wingli.arthas.recorder.RequestRecordHelper@recordHttpRequest(params)'
            {
                WatchCommand watchCommand = new WatchCommand();
                watchCommand.setRegEx(true);
                watchCommand.setClassPattern("org.apache.catalina.connector.CoyoteInputStream|org.springframework.web.method.support.InvocableHandlerMethod|javax.servlet.http.HttpServlet|org.apache.catalina.connector.CoyoteAdapter|com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker");
                watchCommand.setMethodPattern("read|readLine|invokeForRequest|service|postParseRequest|invoke");
                watchCommand.setExpress("@com.wingli.arthas.recorder.WatchEntranceHelper@entrance(clazz.getName(),method.getName(),target,params,returnObj)");
                watchCommand.setMaxNumOfMatchedClass(50);
                watchCommand.setNumberOfLimit(Integer.MAX_VALUE);
                watchCommand.process(process);
                process.appendResult(new RecorderModel("recorder: enhance finish!"));
            }

            //不处理并发
            if (hasChangeGlobalOptions) {
                GlobalOptions.isDisableSubClass = !GlobalOptions.isDisableSubClass;
            }

            //监听处理进度和情况
            {
                RecorderWatcherCommand recorderWatcherCommand = new RecorderWatcherCommand();
                recorderWatcherCommand.setMaxNumOfMatchedClass(1);
                recorderWatcherCommand.process(process);
            }

        } finally {
            process.resumeEnd();
            //process.justShowRecorderView(false);
        }

    }

    private static Object runExpress(Instrumentation inst, String express) {
        ClassLoader classLoader = UserStatUtil.findSuitableClassLoader(inst);
        Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
        try {
            return unpooledExpress.bind(new VmToolCommand.InstancesWrapper(null, null, classLoader)).get(express);
        } catch (ExpressException e) {
            logger.warn("ognl: failed execute express: " + express, e);
            throw new RuntimeException("runExpress fail! " + express);
        }
    }

}
