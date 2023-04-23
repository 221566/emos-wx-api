package com.example.emos.wx.config.shior;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    @Value("${emos.jwt.secret}")
    private String secret;

    @Value("${emos.jwt.expire}")
    private String expire;
//创建令牌的方法
    public String createToken(int userId){
        Date date = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR,5);
//        这里不能直接new对象，而是调用静态工厂方法去创建对象
        Algorithm algorithm = Algorithm.HMAC256(secret);
//        这里是使用内部类来传建的，JWTCreator这是外部类，里面的类是Builder
        JWTCreator.Builder builder = JWT.create();
        String token = builder.withClaim("userId",userId).withExpiresAt(date).sign(algorithm);
        return token;
    }
    //创建令牌的方法
    public int getUserId(String token){
//        对令牌字符串进行解码
        DecodedJWT jwt = JWT.decode(token);
        int userId = jwt.getClaim("userId").asInt();
        return userId;
    }
//  验证令牌有效性
    public void verifierToken(String token){
//        令牌字符串要用到算法所以要创造这个对象
        Algorithm algorithm = Algorithm.HMAC256(secret);
//        用算法对象进行解密build()会帮我们创建一个验证的对象
        JWTVerifier verifier = JWT.require(algorithm).build();
//        验证令牌是否有效
        verifier.verify(token);
    }
}
