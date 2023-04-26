package com.example.emos.wx;

import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.SysConfigDao;
import com.example.emos.wx.db.pojo.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan
@MapperScan("com.example.emos.wx.db.dao")
@Slf4j
public class EmosWxApiApplication {

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private SystemConstants constants;

    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }

    @PostConstruct
    public void init(){
        List<SysConfig> list = sysConfigDao.selectAllParam();
        list.forEach(one->{
            String key = one.getParamKey();
            key = StrUtil.toCamelCase(key);
            String value = one.getParamValue();
            try {
//                通过反射方式给封装类对象赋值，getDeclaredField()，传入常量的名字去封装类找变量
                Field field = constants.getClass().getDeclaredField(key);
//               对封装类进行赋值传入封装类对象constants，然后对变量赋值value
                field.set(constants,value);
            }catch (Exception e){
                log.error("执行异常",e);
            }
        });
    }
}
