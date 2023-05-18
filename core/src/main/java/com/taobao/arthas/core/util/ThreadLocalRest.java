package com.taobao.arthas.core.util;

import com.taobao.arthas.core.advisor.RestAdvice;

public class ThreadLocalRest {

    private final ThreadLocal<RestAdvice> restAdviceRef = new ThreadLocal<RestAdvice>();

    public RestAdvice loadRestAdvice(){
        RestAdvice restAdvice = restAdviceRef.get();
        if (restAdvice == null){
            restAdvice = new RestAdvice();
            restAdvice.setReq(new RestAdvice.Req());
            restAdvice.setResp(new RestAdvice.Resp());
            restAdviceRef.set(restAdvice);
        }
        return restAdviceRef.get();
    }

}