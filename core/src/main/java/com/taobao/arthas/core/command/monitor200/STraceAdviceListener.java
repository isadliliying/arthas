package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.strace.SpanHandler;
import com.taobao.arthas.core.shell.handlers.strace.SpanHandlerHolder;
import com.taobao.arthas.core.util.JsonUtils;
import com.taobao.arthas.core.util.LogUtil;

import java.arthas.SpyAPI;
import java.io.FileOutputStream;
import java.util.UUID;

class STraceAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(STraceAdviceListener.class);
    private STraceCommand command;
    private CommandProcess process;

    public STraceAdviceListener(STraceCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        Advice advice = Advice.newForBefore(loader, clazz, method, target, args);
        if (command.isForTransaction()) {
            doTransactionEnter(advice);
        } else {
            if (SpyAPI.getTraceUid() == null) return;
            doSpanEnter(advice);
        }


    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject);
        if (command.isForTransaction()) {
            doTransactionExit(advice);
        } else {
            if (SpyAPI.getTraceUid() == null) return;
            doSpanExit(advice);
        }


    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        if (command.isForTransaction()) {
            doTransactionExit(advice);
        } else {
            if (SpyAPI.getTraceUid() == null) return;
            doSpanExit(advice);
        }


    }

    private void doSpanEnter(Advice advice) {
        String currentTraceUid = SpyAPI.getTraceUid();
        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();

        SpanHandler handler = SpanHandlerHolder.matchHandler( advice);
        if (handler == null) return;
        Object ognlObj = handler.enterOgnlObj(advice);
        String content = JsonUtils.toJSONString(ognlObj);
        String text = String.format("[span][enter][%s][%s][%s][%s]", System.currentTimeMillis(), clzName, methodName, content);
        appendToFile(currentTraceUid, text);
    }


    private void doSpanExit(Advice advice) {

        String currentTraceUid = SpyAPI.getTraceUid();

        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();

        boolean hasException = advice.getThrowExp() != null;

        SpanHandler handler = SpanHandlerHolder.matchHandler( advice);
        if (handler == null) return;

        Object ognlObj = handler.exitOgnlObj(advice);
        String content = JsonUtils.toJSONString(ognlObj);
        String text = String.format("[span][exit][%s][%s][%s][%s][%s]", hasException, System.currentTimeMillis(), clzName, methodName, content);
        appendToFile(currentTraceUid, text);
    }

    private void doTransactionEnter(Advice advice) {
        process.times().incrementAndGet();
        if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
            abortProcess(process, command.getNumberOfLimit());
            return;
        }

        if (!command.isMatchEntranceInFinally()){
            if (hasConditionNotMatch(advice)){
                return;
            }
        }

        String currentTraceUid = SpyAPI.getTraceUid();
        if (currentTraceUid == null) {
            String traceUid = generateTraceUid();
            SpyAPI.setTraceUid(traceUid);
            currentTraceUid = traceUid;
        }
        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();
        String text = String.format("[transaction][enter][%s][%s][%s]", System.currentTimeMillis(), clzName, methodName);
        appendToFile(currentTraceUid, text);

        process.write(text);
        process.write("\n");


    }

    private void doTransactionExit(Advice advice) {
        String currentTraceUid = SpyAPI.getTraceUid();
        if (currentTraceUid == null) return;

        if (command.isMatchEntranceInFinally()){
            if (hasConditionNotMatch(advice)){
                return;
            }
        }

        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();
        String text = String.format("[transaction][exit][%s][%s][%s]", System.currentTimeMillis(), clzName, methodName);
        appendToFile(currentTraceUid, text);

        process.write(text);
        process.write("\n");

        SpyAPI.setTraceUid(null);

    }

    private static void appendToFile(String traceUid, String text) {
        String logFile = LogUtil.straceDir() + traceUid + ".log";

        try {
            FileOutputStream outputStream = new FileOutputStream(logFile, true);
            try {
                outputStream.write(text.getBytes());
                outputStream.write("\n".getBytes());
            } finally {
                outputStream.close();
            }
        } catch (Exception e) {
            //ignore
        }
    }

    private static String generateTraceUid() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean hasConditionNotMatch(Advice advice){
        try {
            return !isConditionMet(command.getConditionExpress(), advice, 0.0);
        }catch (Exception e){
            logger.warn("strace failed.", e);
            process.end(-1, "watch failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
            return true;
        }
    }

}
