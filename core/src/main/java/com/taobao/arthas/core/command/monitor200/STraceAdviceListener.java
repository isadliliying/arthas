package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.ThreadNode;
import com.taobao.arthas.core.command.model.TraceModel;
import com.taobao.arthas.core.command.model.TraceTree;
import com.taobao.arthas.core.command.view.TraceView;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.strace.SpanHandler;
import com.taobao.arthas.core.shell.handlers.strace.SpanHandlerHolder;
import com.taobao.arthas.core.util.*;
import com.taobao.arthas.core.view.TreeView;

import java.arthas.SpyAPI;
import java.io.*;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class STraceAdviceListener extends AdviceListenerAdapter {

    private static final ThreadLocal<AtomicInteger> tagCountRef = new ThreadLocal<AtomicInteger>();

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

        SpanHandler handler = SpanHandlerHolder.matchHandler(advice, command.isCreateByBiz());
        if (handler == null) return;
        int count = tagCountRef.get().incrementAndGet();

        Object ognlObj = handler.enterOgnlObj(advice);
        String mark = clzName + "#" + methodName;
        String content = JsonUtils.toJSONString(ognlObj);
        String text = String.format("[span][enter][%s][%s][%s]", System.nanoTime(), mark, content);
        text = padWithTab(text, count);
        appendToFile(currentTraceUid, text);
    }


    private void doSpanExit(Advice advice) {

        String currentTraceUid = SpyAPI.getTraceUid();

        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();

        SpanHandler handler = SpanHandlerHolder.matchHandler(advice, command.isCreateByBiz());
        if (handler == null) return;

        int count = tagCountRef.get().getAndDecrement();
        Object exitOgnlObj = handler.exitOgnlObj(advice);
        String content = JsonUtils.toJSONString(exitOgnlObj);
        String mark = clzName + "#" + methodName;
        String text = String.format("[span][exit][%s][%s][%s]", System.nanoTime(), mark, content);
        text = padWithTab(text, count);
        appendToFile(currentTraceUid, text);

    }

    private void doTransactionEnter(Advice advice) {
        if (!command.isMatchEntranceInFinally()) {
            if (hasConditionNotMatch(advice)) {
                return;
            }
        }

        String currentTraceUid = SpyAPI.getTraceUid();
        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();
        if (currentTraceUid == null) {
            String traceUid = generateTraceUid(clzName, methodName);
            SpyAPI.setTraceUid(traceUid);
            currentTraceUid = traceUid;
            process.times().incrementAndGet();
            if (isLimitExceeded(command.getNumberOfLimit() + 1, process.times().get())) {
                abortProcess(process, command.getNumberOfLimit());
                return;
            }
        } else {
            return;
        }

        process.write("transaction:" + currentTraceUid + " is beginning.");
        process.write("\n");

        HashMap<String, Object> enterMap = new HashMap<String, Object>();
        enterMap.put("args", advice.getParams());
        String argText = JsonUtils.toJSONString(enterMap);
        String mark = clzName + "#" + methodName;
        String text = String.format("[transaction][enter][%s][%s][%s]", System.nanoTime(), mark, argText);
        appendToFile(currentTraceUid, text);


        tagCountRef.set(new AtomicInteger(2));
    }

    private void doTransactionExit(Advice advice) {
        String currentTraceUid = SpyAPI.getTraceUid();
        if (currentTraceUid == null) return;

        if (command.isMatchEntranceInFinally()) {
            if (hasConditionNotMatch(advice)) {
                return;
            }
        }

        String clzName = advice.getClazz().getSimpleName();
        String methodName = advice.getMethod().getName();
        HashMap<String, Object> exitMap = new HashMap<String, Object>();
        exitMap.put("returnObj", advice.getReturnObj());
        String returnText = JsonUtils.toJSONString(exitMap);
        String mark = clzName + "#" + methodName;
        String text = String.format("[transaction][exit][%s][%s][%s]", System.nanoTime(), mark, returnText);
        appendToFile(currentTraceUid, text);

        SpyAPI.setTraceUid(null);

        //do transform
        String logFile = getLogFileName(currentTraceUid);
        String jsonFile = getJsonFileName(currentTraceUid);

        ThreadNode threadNode = ThreadUtil.getThreadNode(advice.getLoader(), Thread.currentThread());
        TraceTree tree = StraceUtils.transformFile(logFile, jsonFile, threadNode);
        TraceView traceView = new TraceView();
        String treeStr = traceView.drawTree(tree.getRoot());
        process.write(treeStr);

        process.write("transaction:" + currentTraceUid + " is end.\n");
        process.write("logFile:" + logFile + "\n");
        process.write("jsonFile:" + jsonFile + "\n");
        process.write("\n");
    }

    private static String getLogFileName(String traceUid) {
        return LogUtil.straceDir() + traceUid + ".log";
    }

    private static String getJsonFileName(String traceUid) {
        return LogUtil.straceDir() + traceUid + ".json";
    }

    private static void appendToFile(String traceUid, String text) {
        String logFile = getLogFileName(traceUid);
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

    private static String generateTraceUid(String clzName, String methodName) {
        return System.nanoTime() + "-" + clzName + "-" + methodName + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean hasConditionNotMatch(Advice advice) {
        try {
            return !isConditionMet(command.getConditionExpress(), advice, 0.0);
        } catch (Exception e) {
            logger.warn("strace failed.", e);
            process.end(-1, "watch failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
            return true;
        }
    }

    private static String padWithTab(String text, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("\t");
        }
        return sb.append(text).toString();
    }

    public STraceCommand getCommand() {
        return command;
    }

    public static Pair<String, Integer> findNextBlock(String text, int begin) {
        int fromIdx = text.indexOf('[', begin);
        if (fromIdx < 0) return null;
        int endIdx = text.indexOf(']', fromIdx + 1);
        if (endIdx < 0) return null;
        return Pair.make(text.substring(fromIdx, endIdx), endIdx);
    }


}
