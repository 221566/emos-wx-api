package com.example.emos.wx.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (!StrUtil.hasEmpty(value)){
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null){
            for (int i = 0;i < values.length;i++){
                String value = values[i];
                if (!StrUtil.hasEmpty(value)){
                    value = HtmlUtil.filter(value);
                }
                values[i] = value;
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        LinkedHashMap<String, String[]> map = new LinkedHashMap();
        if (parameters != null){
            for (String key : parameters.keySet()) {
                   String[] values = parameters.get(key);
                   for (int i = 0; i < values.length; i++){
                       String value = values[i];
                       if (!StrUtil.hasEmpty(value)){
                           value = HtmlUtil.filter(value);
                       }
                       values[i] = value;
                   }
                   map.put(key,values);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (!StrUtil.hasEmpty(value)){
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        //请求里面读数据的io流
        InputStream in = super.getInputStream();
//      创建字符流从InputStream流里读取数据
        InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
//        读取数据的高效性可以创建个缓冲流
        BufferedReader buffer = new BufferedReader(reader);
//        从缓存流读取的数据是用保存起来的，定义一个StringBuffer因为要做字符串拼接
        StringBuffer bod = new StringBuffer();
//        从缓冲流读取第一行数据出来
        String line = buffer.readLine();
//        判断读取的第一行数据是否有效
        while (line != null){
//            拼接起来
            bod.append(line);
//            读取第二行数据
            line = buffer.readLine();
        }
//        数据读取出来后把流都关闭掉
        buffer.close();
        reader.close();
        in.close();
//       把读取出来的内容进行数据类型的转换，客户端提交的内容是json格式的，所以读取出来的字符串内容是json数据
//        在java里面是不支持原生json格式的，所以要把它转换成map对象
        Map<String,Object> map = JSONUtil.parseObj(bod.toString());
        Map<String,Object> result = new LinkedHashMap<>();
//       从map，通过key获取v，然后判断是否是有效的字符串，如果是的话就进行转义
        for (String key : map.keySet()){
            Object val = map.get(key);
//            判断是否是字符串格式数据
            if (val instanceof String){
//                如果是字符串格式数据，就转换成字符串val.toString()，然后在进行转义
                if (!StrUtil.hasEmpty(val.toString())){
                    result.put(key,HtmlUtil.filter(val.toString()));
                }else {
//                    不需要转义的直接丢进去
                    result.put(key,val);
                }
            }
        }
        /**
         * result已经保存了该转义的数据，然后该怎么把这个数据通过io流进行返回
         * 可以先转成json格式的字符串，然后在传建一个io流从字符串读数据，把
         * 这个io流返回
         */
        String json = JSONUtil.toJsonStr(result);
//            创建一个io流从字符串里面读数据返回
        ByteArrayInputStream bain = new ByteArrayInputStream(json.getBytes());
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener listener) {

            }

            @Override
            public int read() throws IOException {
                return bain.read();
            }
        };
    }
}
