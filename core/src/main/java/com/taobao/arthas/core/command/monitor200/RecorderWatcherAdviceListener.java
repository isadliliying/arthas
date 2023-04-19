package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.model.RecorderModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.net.URI;

class RecorderWatcherAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RecorderWatcherAdviceListener.class);
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private RecorderWatcherCommand command;
    private CommandProcess process;

    public RecorderWatcherAdviceListener(RecorderWatcherCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        watching(args);
    }

    /**
     * 返回通知
     *
     * @param loader       类加载器
     * @param clazz        类
     * @param method       方法
     * @param target       目标类实例 若目标为静态方法,则为null
     * @param args         参数列表
     * @param returnObject 返回结果 若为无返回值方法(void),则为null
     * @throws Throwable 通知过程出错
     */
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {

    }

    /**
     * 异常通知
     *
     * @param loader    类加载器
     * @param clazz     类
     * @param method    方法
     * @param target    目标类实例 若目标为静态方法,则为null
     * @param args      参数列表
     * @param throwable 目标异常
     * @throws Throwable 通知过程出错
     */
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {

    }


    private void watching(Object[] args) {
        try {
            Object param = args[0];
            Object current = args[1];

            JSONObject requestObj = JSON.parseObject(JSON.toJSONString(param));
            String taskName = requestObj.getString("taskName");
            String requestType = requestObj.getString("requestType");

            if (requestType.equals("http")){
                String url = requestObj.getString("httpRequestUrl");
                String method = requestObj.getString("httpRequestMethod");
                URI uri = new URI(url);
                String urlWithoutQuery = uri.getRawPath();

                String printContent = String.format("[%d][%s][%s][%s][%s]", current,taskName,requestType, method, urlWithoutQuery);
                process.appendResult(new RecorderModel(printContent));
            }else {
                String dubboInterface = requestObj.getString("dubboInterface");
                String dubboMethod = requestObj.getString("dubboMethod");

                String printContent = String.format("[%d][%s][%s][%s][%s]", current,taskName,requestType, dubboInterface, dubboMethod);
                process.appendResult(new RecorderModel(printContent));
            }

        } catch (Throwable e) {
            logger.warn("recorder watch failed.", e);
            process.end(-1, "recorder watch failed, condition is: " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }
}
