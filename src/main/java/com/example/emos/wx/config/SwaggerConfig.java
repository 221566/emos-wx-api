package com.example.emos.wx.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket createRestApi(){
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
        ApiInfoBuilder builder = new ApiInfoBuilder();
        builder.title("EMOS在线办公系统");
//        ApiInfo不是new出来的而是通过builder.build();
        ApiInfo info = builder.build();
        docket.apiInfo(info);
//      ApiSelectorBuilder是通过docket.select();构建出来的
        ApiSelectorBuilder selectorBuilder = docket.select();
        /**
         * PathSelectors.any()所有包
         * withMethodAnnotation(ApiOperation.class)有ApiOperation注解的方法才会添加
         */
        selectorBuilder.paths(PathSelectors.any());
        selectorBuilder.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class));
        docket = selectorBuilder.build();

//         支持jwt每次发起请求都要上传令牌字符串
        /**
         *header 请求头里面接受客服端上传的令牌
         * 封装到list里面
         * 然后把list添加到swagger
         */
        ApiKey apiKey = new ApiKey("token","token","header");
        List<ApiKey> apiKeyList = new ArrayList<>();
        apiKeyList.add(apiKey);
        docket.securitySchemes(apiKeyList);
        /**
         * 令牌作用域
         * 封装到数组里面
         *  把数组添加到SecurityReference里面
         *  把SecurityReference封装到list里面
         *  再把封装的list添加到SecurityContext里面
         *  然后把SecurityContext进行list封装
         *  最后添加到docket
         */
        AuthorizationScope scope = new AuthorizationScope("global","accessEverything");
        AuthorizationScope[] scopes = {scope};
        SecurityReference reference = new SecurityReference("token",scopes);
        List references = new ArrayList<>();
        references.add(reference);
        SecurityContext context = SecurityContext.builder().securityReferences(references).build();
        List cxtList = new ArrayList();
        cxtList.add(context);
        docket.securityContexts(cxtList);
        return docket;
    }
}
