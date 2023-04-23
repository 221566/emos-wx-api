package com.example.emos.wx.aop;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shior.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TokenAspect {
    @Autowired
    private ThreadLocalToken threadLocalToken;
//切点方法
    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..))")
    public void aspect(){

    }
//给切点加上环绕事件
    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
//        point.proceed();拦截方法返回执行结果
       R r = (R)point.proceed();
        String token = threadLocalToken.getToken();
        if (token != null){
            r.put("token",token);
            threadLocalToken.clear();
        }
        return r;
    }
}
