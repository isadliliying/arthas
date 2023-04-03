package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author beiwei30 on 29/11/2016.
 */
public class TraceAdviceListener extends AbstractTraceAdviceListener implements InvokeTraceable {

    /**
     * Constructor
     */
    public TraceAdviceListener(TraceCommand command, CommandProcess process, boolean verbose) {
        super(command, process);
        super.setVerbose(verbose);
    }

    /**
     * trace 会在被观测的方法体中，在每个方法调用前后插入字节码，所以方法调用开始，结束，抛异常的时候，都会回调下面的接口
     */
    @Override
    public void invokeBeforeTracing(ClassLoader classLoader, String tracingClassName, String tracingMethodName, String tracingMethodDesc, int tracingLineNumber)
            throws Throwable {
        if (this.command.isShowBefore()) {
            process.write(String.format("[%s] [before] %s:%s() #%s\n", System.currentTimeMillis(),tracingClassName.replace("/","."), tracingMethodName, tracingLineNumber));
        }
        // normalize className later
        threadLocalTraceEntity(classLoader).tree.begin(tracingClassName, tracingMethodName, tracingLineNumber, true);
    }

    @Override
    public void invokeAfterTracing(ClassLoader classLoader, String tracingClassName, String tracingMethodName, String tracingMethodDesc, int tracingLineNumber)
            throws Throwable {
        if (this.command.isShowBefore()) {
            process.write(String.format("[%s] [after] %s:%s() #%s\n", System.currentTimeMillis(),tracingClassName.replace("/","."), tracingMethodName, tracingLineNumber));
        }
        threadLocalTraceEntity(classLoader).tree.end();
    }

    @Override
    public void invokeThrowTracing(ClassLoader classLoader, String tracingClassName, String tracingMethodName, String tracingMethodDesc, int tracingLineNumber)
            throws Throwable {
        if (this.command.isShowBefore()) {
            process.write(String.format("[%s] [exception] %s:%s() #%s\n", System.currentTimeMillis(),tracingClassName.replace("/","."), tracingMethodName, tracingLineNumber));
        }
        threadLocalTraceEntity(classLoader).tree.end(true);
    }

}
