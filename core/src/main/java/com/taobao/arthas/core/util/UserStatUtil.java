package com.taobao.arthas.core.util;

import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Arthas 使用情况统计
 * <p/>
 * Created by zhuyong on 15/11/12.
 */
public class UserStatUtil {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final byte[] SKIP_BYTE_BUFFER = new byte[DEFAULT_BUFFER_SIZE];

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "arthas-UserStat");
            t.setDaemon(true);
            return t;
        }
    });
    private static final String ip = IPUtils.getLocalIP();

    private static final String version = URLEncoder.encode(ArthasBanner.version().replace("\n", ""));

    private static volatile String statUrl = null;

    public static String getStatUrl() {
        return statUrl;
    }

    public static void setStatUrl(String url) {
        statUrl = url;
    }

    public static void arthasStart(Instrumentation inst) {
        RemoteJob job = new RemoteJob();
        job.appendQueryData("env", getEnv(inst));
        job.appendQueryData("appid", getAppId(inst));
        job.appendQueryData("ip", ip);
        job.appendQueryData("command", "start");

        try {
            executorService.execute(job);
        } catch (Throwable t) {
            //
        }
    }

    public static void arthasUsage(String cmd, String detail, Instrumentation inst) {
        RemoteJob job = new RemoteJob();
        job.appendQueryData("env", getEnv(inst));
        job.appendQueryData("appid", getAppId(inst));
        job.appendQueryData("ip", ip);
        job.appendQueryData("command", URLEncoder.encode(cmd));
        if (detail != null) {
            job.appendQueryData("arguments", URLEncoder.encode(detail));
        }

        try {
            executorService.execute(job);
        } catch (Throwable t) {
            //
        }
    }

    public static void arthasUsage(String cmd, List<String> args, Instrumentation inst) {
        StringBuilder commandString = new StringBuilder(cmd);
        for (String arg : args) {
            commandString.append(" ").append(arg);
        }
        UserStatUtil.arthasUsage(cmd, commandString.toString(),inst);
    }

    /**
     * 获取环境
     */
    public static String getEnv(Instrumentation inst){
        try {
            ClassLoader classLoader = findSuitableClassLoader(inst);
            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
            String express = "@com.cvte.psd.foundation.Foundation@server().getEnvType()";
            return unpooledExpress.get(express).toString();
        }catch (Throwable t){
            //ignore
        }
        return "";
    }

    public static ClassLoader findSuitableClassLoader(Instrumentation inst){
        String defaultClassLoaderName = "org.springframework.boot.loader.LaunchedURLClassLoader";
        String backupClassLoaderName = "jdk.internal.loader.ClassLoaders$AppClassLoader";
        List<ClassLoader> defaultClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, defaultClassLoaderName);
        List<ClassLoader> backupClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, backupClassLoaderName);
        defaultClassLoaders.addAll(backupClassLoaders);

        if (defaultClassLoaders.isEmpty()){
            return null;
        }
        return defaultClassLoaders.get(0);
    }

    /**
     * 获取appId
      */
    public static String getAppId(Instrumentation inst){
        try {
            ClassLoader classLoader = findSuitableClassLoader(inst);
            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
            String express = "@com.cvte.psd.foundation.Foundation@app().getAppId()";
            return unpooledExpress.get(express).toString();
        }catch (Throwable t){
            //ignore
        }
        return "";
    }

    public static void destroy() {
        // 直接关闭，没有回报的丢弃
        executorService.shutdownNow();
    }

    static class RemoteJob implements Runnable {
        private StringBuilder queryData = new StringBuilder();

        public void appendQueryData(String key, String value) {
            if (key != null && value != null) {
                if (queryData.length() == 0) {
                    queryData.append(key).append("=").append(value);
                } else {
                    queryData.append("&").append(key).append("=").append(value);
                }
            }
        }

        @Override
        public void run() {
            String link = statUrl;
            if (link == null) {
                return;
            }
            InputStream inputStream = null;
            try {
                if (queryData.length() != 0) {
                    link = link + "?" + queryData;
                }
                URL url = new URL(link);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                inputStream = connection.getInputStream();
                //noinspection StatementWithEmptyBody
                while (inputStream.read(SKIP_BYTE_BUFFER) != -1) {
                    // do nothing
                }
            } catch (Throwable t) {
                // ignore
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }
}
