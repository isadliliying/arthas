package com.taobao.arthas.core.util;

import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.binding.StringBinding;
import com.alibaba.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.BindingParserUtils;
import com.alibaba.bytekit.utils.ReflectionUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.taobao.arthas.core.advisor.SpyInterceptors;
import com.taobao.arthas.core.command.monitor200.LookLocationMatcher;

import java.lang.reflect.Method;
import java.util.List;

public class LookInterceptorParserUtils {

    public static InterceptorProcessor createLookInterceptorProcessor(String locCode) {
        Method method = ReflectionUtils.findMethod(SpyInterceptors.SpyLookInterceptor.class, "atLineBefore",null);

        InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());

        //locationMatcher
        interceptorProcessor.setLocationMatcher(new LookLocationMatcher(locCode));

        //interceptorMethodConfig
        InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
        interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);
        interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
        interceptorMethodConfig.setMethodName(method.getName());
        interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

        //inline
        interceptorMethodConfig.setInline(true);

        //bindings
        List<Binding> bindings = BindingParserUtils.parseBindings(method);
        for (Binding binding : bindings) {
            //因为注解值不能动态变化，所以需要在这里进行重新赋值，其实在这里生成binding也可以，但是方法注解很方便
            if (binding instanceof StringBinding){
                StringBinding stringBinding = (StringBinding) binding;
                stringBinding.setValue(locCode);
            }
        }
        interceptorMethodConfig.setBindings(bindings);

        return interceptorProcessor;
    }
}
