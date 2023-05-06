package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import ognl.ClassResolver;
import ognl.DefaultMemberAccess;
import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlRuntime;

/**
 * @author ralf0131 2017-01-04 14:41.
 * @author hengyunabc 2018-10-18
 */
public class OgnlExpress implements Express {
    private static final MemberAccess MEMBER_ACCESS = new DefaultMemberAccess(true);
    private static final Logger logger = LoggerFactory.getLogger(OgnlExpress.class);
    private static final ArthasObjectPropertyAccessor OBJECT_PROPERTY_ACCESSOR = new ArthasObjectPropertyAccessor();

    private static final ArthasMethodAccessor METHOD_ACCESSOR = new ArthasMethodAccessor();

    private Object bindObject;
    private final OgnlContext context;

    public OgnlExpress() {
        this(CustomClassResolver.customClassResolver);
    }

    public OgnlExpress(ClassResolver classResolver) {
        //
        OgnlRuntime.setPropertyAccessor(Object.class, OBJECT_PROPERTY_ACCESSOR);
        OgnlRuntime.setMethodAccessor(Object.class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(Object.class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(byte[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(short[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(char[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(int[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(long[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(float[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(double[].class, METHOD_ACCESSOR);
        OgnlRuntime.setMethodAccessor(Object[].class, METHOD_ACCESSOR);
        context = new OgnlContext();
        context.setClassResolver(classResolver);
        // allow private field access
        context.setMemberAccess(MEMBER_ACCESS);
    }

    @Override
    public Object get(String express) throws ExpressException {
        try {
            return Ognl.getValue(express, context, bindObject);
        } catch (Exception e) {
            logger.error("Error during evaluating the expression:", e);
            throw new ExpressException(express, e);
        }
    }

    @Override
    public boolean is(String express) throws ExpressException {
        final Object ret = get(express);
        return ret instanceof Boolean && (Boolean) ret;
    }

    @Override
    public Express bind(Object object) {
        this.bindObject = object;
        return this;
    }

    @Override
    public Express bind(String name, Object value) {
        context.put(name, value);
        return this;
    }

    @Override
    public Express reset() {
        context.clear();
        context.setClassResolver(CustomClassResolver.customClassResolver);
        // allow private field access
        context.setMemberAccess(MEMBER_ACCESS);
        return this;
    }
}
