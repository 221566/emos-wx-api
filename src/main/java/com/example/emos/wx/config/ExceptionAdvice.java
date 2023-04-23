package com.example.emos.wx.config;

import com.example.emos.wx.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {
//方法返回的字符串就是错误消息，是要写到响应里面的所以要加上 @ResponseBody
//@ExceptionHandler(Exception.class)可以捕获Exception异常
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String exceptionHandler(Exception e){
        log.error("执行异常",e);
        if(e instanceof MethodArgumentNotValidException){
            MethodArgumentNotValidException exception= (MethodArgumentNotValidException) e;
//            获取具体异常消息，如某一个字段不通过它的具体原因是什么
            return exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        else if(e instanceof EmosException){
            EmosException exception= (EmosException) e;
            return exception.getMsg();
        }
        else if(e instanceof UnauthorizedException){
            return "你不具备相关权限";
        }
        else{
            return "后端执行异常";
        }
    }

}
