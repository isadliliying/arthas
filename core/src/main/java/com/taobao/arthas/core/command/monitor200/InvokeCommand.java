package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.*;

import java.lang.instrument.Instrumentation;
import java.util.List;

@Name("invoke")
@Summary("replace vmtool --action getInstances --className org.springframework.context.ApplicationContext")
@Description(Constants.EXAMPLE
        + "  invoke demo.MathGame 'instances[0]'\n"
        + "  invoke demo.MathGame 'instances.length'\n"
        + "  invoke demo.MathGame demo.MathGame -x 2\n"
        + "  invoke demo.MathGame java.lang.String --limit 10\n"
        + Constants.WIKI + Constants.WIKI_HOME + "invoke")
//@formatter:on
public class InvokeCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(InvokeCommand.class);

    private String className;
    private String express;

    private String hashCode = null;
    private String classLoaderClass;
    /**
     * default value 1
     */
    private int expand;

    /**
     * default value 10
     */
    private int limit;

    private String libPath;

    private String traceUid;

    @Argument(index = 0, argName = "class-name")
    @Description("The class name")
    public void setClassName(String className) {
        this.className = className;
    }

    @Argument(index = 1, argName = "express", required = false)
    @DefaultValue("instances")
    @Description("The ognl expression, default value is `instances`.")
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    @DefaultValue("1")
    public void setExpand(int expand) {
        this.expand = expand;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "l", longName = "limit")
    @Description("Set the limit value of the getInstances action, default value is 10, set to -1 is unlimited")
    @DefaultValue("10")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Option(shortName = "t", longName = "traceUid")
    @Description("Set the limit value of the getInstances action, default value is 10, set to -1 is unlimited")
    public void setTraceUid(String traceUid) {
        this.traceUid = traceUid;
    }

    @Option(longName = "libPath")
    @Description("The specify lib path.")
    public void setLibPath(String path) {
        libPath = path;
    }


    @Override
    public void process(final CommandProcess process) {
        //目前好像都是每次使用时创建，没有保存，不知道为啥
        VmToolCommand vmToolCommand = new VmToolCommand();
        vmToolCommand.setAction(VmToolCommand.VmToolAction.getInstances);
        vmToolCommand.setClassName(this.className);
        if (StringUtils.isBlank(classLoaderClass)) {
            String defaultClassLoaderName = "org.springframework.boot.loader.LaunchedURLClassLoader";
            Instrumentation inst = process.session().getInstrumentation();
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, defaultClassLoaderName);
            if (matchedClassLoaders == null || !matchedClassLoaders.isEmpty()) {
                vmToolCommand.setClassLoaderClass(defaultClassLoaderName);
            }
        } else {
            vmToolCommand.setClassLoaderClass(this.classLoaderClass);
        }
        String newExpress = "@java.lang.Thread@currentThread().setContextClassLoader(classLoader)," + express;
        if (!StringUtils.isBlank(traceUid)){
            newExpress = "@com.seewo.honeycomb.log.LogContextHolder@setTraceId(\""+traceUid+"\"),"+newExpress;
        }
        vmToolCommand.setExpress(newExpress);
        vmToolCommand.setExpand(this.expand);
        vmToolCommand.setHashCode(this.hashCode);
        vmToolCommand.setLimit(this.limit);
        vmToolCommand.setLibPath(this.libPath);
        vmToolCommand.process(process);
    }

}
