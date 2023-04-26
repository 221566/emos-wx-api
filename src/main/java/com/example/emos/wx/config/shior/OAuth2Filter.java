package com.example.emos.wx.config.shior;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//@Scope("prototype") 开启多例对象
@Component
@Scope("prototype")
public class OAuth2Filter extends AuthenticatingFilter {

    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

//    拦截请求后，用于把令牌字符串封装成令牌对象
    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req =(HttpServletRequest) request;
        String token = getRequestToken(req);
        if (StrUtil.isBlank(token)){
            return null;
        }
//        封装成令牌对象
        return new OAuth2Token(token);
    }
//判断请求是否需要被shiro处理
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req =(HttpServletRequest) request;
//        req.getMethod()用于判断请求的类型是post或者get等
        if (req.getMethod().equals(RequestMethod.OPTIONS.name())){
//            是OPTIONS就不会由shior处理
            return true;
        }
        return false;
    }
//该方法用于处理所有应被shiro处理的请求
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req =(HttpServletRequest) request;
        HttpServletResponse resp =(HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
//        跨域参数，允许跨域
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        threadLocalToken.clear();
        String token = getRequestToken(req);
        if (StrUtil.isBlank(token)){
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
/**
 * 这是 Java 代码中用于向客户端输出文本的语句。具体来说，
 * resp 是 HttpServletResponse 对象的实例，getWriter()
 * 方法返回一个 PrintWriter 对象，而 print() 方法则将文本内容输出到客户端。
 */
            resp.getWriter().print("无效令牌");
            return false;
        }
        try {
            jwtUtil.verifierToken(token);
        }catch (TokenExpiredException e){
//            判断redis缓存里面的令牌是否过期
            if (redisTemplate.hasKey(token)){
//                没有过期就删除老令牌，然后生成新的一起返回给媒介
                redisTemplate.delete(token);
//                通过老令牌获得出userId
                int userId = jwtUtil.getUserId(token);
//                生成新令牌
                token = jwtUtil.createToken(userId);
                redisTemplate.opsForValue().set(token,userId+"",cacheExpire, TimeUnit.DAYS);
                threadLocalToken.setToken(token);
            }else {
                resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
                resp.getWriter().print("令牌已过期");
                return false;
            }
        }catch (Exception e){
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效令牌");
            return false;
        }
        boolean bool = executeLogin(request, response);

        return bool;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        HttpServletRequest req= (HttpServletRequest) request;
        HttpServletResponse resp= (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
        try{
            resp.getWriter().print(e.getMessage());
        }catch (Exception exception){

        }

        return false;
    }

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest req= (HttpServletRequest) request;
        HttpServletResponse resp= (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        super.doFilterInternal(request, response, chain);

    }

    private String getRequestToken(HttpServletRequest request){
//        从请求头获取token
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)){
//            如果请求头没有数据，就从请求体获取
            token = request.getParameter("token");
        }
        return token;
    }
}
