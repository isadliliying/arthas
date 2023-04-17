package com.wingli.arthas.recorder;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchEntranceHelper {

    private static final Logger logger = LoggerFactory.getLogger(RequestRecordHelper.class);

    /**
     * 统一 watch 入口
     */
    public static void entrance(String clzName, String methodName, Object target, Object[] params, Object returnObj) {
        try {
            //org.apache.catalina.connector.CoyoteInputStream|org.apache.catalina.connector.CoyoteOutputStream|org.springframework.web.method.support.InvocableHandlerMethod|javax.servlet.http.HttpServlet|org.apache.catalina.connector.CoyoteAdapter read|readLine|write|invokeForRequest|service|postParseRequest

            //watch org.apache.catalina.connector.CoyoteInputStream read '@com.wingli.arthas.recorder.CoyoteInputStreamHelper@recordRead(target,params,returnObj)
            if ("org.apache.catalina.connector.CoyoteInputStream".equals(clzName) && "read".equals(methodName)) {
                CoyoteInputStreamHelper.recordRead(target, params, returnObj);
                return;
            }
            //watch org.apache.catalina.connector.CoyoteInputStream readLine '@com.wingli.arthas.recorder.CoyoteInputStreamHelper@recordReadLine(target,params,returnObj)
            if ("org.apache.catalina.connector.CoyoteInputStream".equals(clzName) && "readLine".equals(methodName)) {
                CoyoteInputStreamHelper.recordReadLine(target, params, returnObj);
                return;
            }
            //watch org.apache.catalina.connector.CoyoteOutputStream write '@com.wingli.arthas.recorder.CoyoteOutputStreamHelper@recordWrite(target,params)
            if ("org.apache.catalina.connector.CoyoteOutputStream".equals(clzName) && "write".equals(methodName)) {
                CoyoteOutputStreamHelper.recordWrite(target, params);
                return;
            }
            //watch org.springframework.web.method.support.InvocableHandlerMethod invokeForRequest '@com.wingli.arthas.recorder.RequestRecordHelper@recordCommonTmp()
            if ("org.springframework.web.method.support.InvocableHandlerMethod".equals(clzName) && "invokeForRequest".equals(methodName)) {
                RequestRecordHelper.recordCommonTmp();
                return;
            }
            //watch javax.servlet.http.HttpServlet service '@com.wingli.arthas.recorder.RequestRecordHelper@recordHttpResponse(params)
            if ("javax.servlet.http.HttpServlet".equals(clzName) && "service".equals(methodName)) {
                RequestRecordHelper.recordHttpResponse(params);
                return;
            }
            //watch org.apache.catalina.connector.CoyoteAdapter postParseRequest '@com.wingli.arthas.recorder.RequestRecordHelper@recordHttpRequest(params)
            if ("org.apache.catalina.connector.CoyoteAdapter".equals(clzName) && "postParseRequest".equals(methodName)) {
                RequestRecordHelper.recordHttpRequest(params);
                return;
            }
            //watch com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker invoke '@com.wingli.arthas.recorder.RequestRecordHelper@receiveDubboRequestEnd(this,$1,$_)
            if ("com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker".equals(clzName) && "invoke".equals(methodName)) {
                if (target instanceof Invoker && params.length == 1 && params[0] instanceof Invocation && returnObj instanceof Result) {
                    Invoker invoker = (Invoker) target;
                    Invocation invocation = (Invocation) params[0];
                    Result result = (Result) returnObj;
                    RequestRecordHelper.receiveDubboRequestEnd(invoker, invocation, result);
                }
                return;
            }
        } catch (Throwable t) {
            logger.error("entrance err.", t);
        }
    }

}
