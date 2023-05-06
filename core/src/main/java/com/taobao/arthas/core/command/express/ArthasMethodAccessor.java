package com.taobao.arthas.core.command.express;

import com.alibaba.fastjson.JSON;
import ognl.*;

import java.util.List;
import java.util.Map;

public class ArthasMethodAccessor extends ObjectMethodAccessor {

    /* MethodAccessor interface */
    public Object callStaticMethod(Map context, Class targetClass, String methodName, Object[] args)
            throws MethodFailedException {
        List methods = OgnlRuntime.getMethods(targetClass, methodName, true);


        return OgnlRuntime.callAppropriateMethod((OgnlContext) context, targetClass,
                null, methodName, null, methods, args);
    }

    public Object callMethod(Map context, Object target, String methodName, Object[] args)
            throws MethodFailedException {
        Class targetClass = (target == null) ? null : target.getClass();
        List methods = OgnlRuntime.getMethods(targetClass, methodName, false);

        if ((methods == null) || (methods.size() == 0)) {
            methods = OgnlRuntime.getMethods(targetClass, methodName, true);
        }

        if (context instanceof OgnlContext && "jstr".equals(methodName) && args.length == 0 && ((methods == null) || (methods.size() == 0))) {
            try {
                Class jsonClass = OgnlRuntime.classForName((OgnlContext) context, "com.alibaba.fastjson.JSON");
                return callStaticMethod(context,jsonClass,"toJSONString",new Object[]{target});
            } catch (ClassNotFoundException e) {
                //ignore
            }
        }

        if (context instanceof OgnlContext && "json".equals(methodName) && args.length == 0 && ((methods == null) || (methods.size() == 0))) {
            try {
                Class jsonClass = OgnlRuntime.classForName((OgnlContext) context, "com.alibaba.fastjson.JSON");
                return callStaticMethod(context,jsonClass,"toJSON",new Object[]{target});
            } catch (ClassNotFoundException e) {
                //ignore
            }
        }

        return OgnlRuntime.callAppropriateMethod((OgnlContext) context, target,
                target, methodName, null, methods, args);
    }

}
