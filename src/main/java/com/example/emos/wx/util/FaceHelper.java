package com.example.emos.wx.util;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.emos.wx.common.util.R;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * 人脸识别工具类
 * @author ma_yi
 * 个人博客 https://blog.csdn.net/qq_23412263
 */
public class FaceHelper {
    // 调用 API
    private static final String FACE_URL ="https://api-cn.faceplusplus.com/facepp/v3/";

    public static final String FACE_API_DETECT = "detect";
    public static final String FACE_API_COMPARE = "compare";

    // 你的 key
    public static final String API_KEY = "qQB5W7m6tyjk9lm7BSxc7fgtt-coX5cs";
    // 你的 SECRET
    private static final String API_SECRET = "NiUjILkiF3XufbAfJoTDY77kFhhqz35v";

    private final static int CONNECT_TIME_OUT = 30000;
    private final static int READ_OUT_TIME = 50000;
    private static String boundaryString = getBoundary();
    public static String faceId;

    public static byte[] post(String api, HashMap<String, String> map, HashMap<String, byte[]> fileMap) throws Exception {
        HttpURLConnection conne;
        URL url1 = new URL(FACE_URL+api);
        conne = (HttpURLConnection) url1.openConnection();
        conne.setDoOutput(true);
        conne.setUseCaches(false);
        conne.setRequestMethod("POST");
        conne.setConnectTimeout(CONNECT_TIME_OUT);
        conne.setReadTimeout(READ_OUT_TIME);
        conne.setRequestProperty("accept", "*/*");
        conne.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);
        conne.setRequestProperty("connection", "Keep-Alive");
        conne.setRequestProperty("user-agent", "Mozilla/4.0 (compatible;MSIE 6.0;Windows NT 5.1;SV1)");
        DataOutputStream obos = new DataOutputStream(conne.getOutputStream());
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            String value = entry.getValue();
            obos.writeBytes("--" + boundaryString + "\r\n");
            obos.writeBytes("Content-Disposition: form-data; name=\"" + key
                    + "\"\r\n");
            obos.writeBytes("\r\n");
            obos.writeBytes(value + "\r\n");
        }
        if (fileMap != null && fileMap.size() > 0) {
            Iterator fileIter = fileMap.entrySet().iterator();
            while (fileIter.hasNext()) {
                Map.Entry<String, byte[]> fileEntry = (Map.Entry<String, byte[]>) fileIter.next();
                obos.writeBytes("--" + boundaryString + "\r\n");
                obos.writeBytes("Content-Disposition: form-data; name=\"" + fileEntry.getKey()
                        + "\"; filename=\"" + encode(" ") + "\"\r\n");
                obos.writeBytes("\r\n");
                obos.write(fileEntry.getValue());
                obos.writeBytes("\r\n");
            }
        }
        obos.writeBytes("--" + boundaryString + "--" + "\r\n");
        obos.writeBytes("\r\n");
        obos.flush();
        obos.close();
        InputStream ins = null;
        int code = conne.getResponseCode();
        try {
            if (code == 200) {
                ins = conne.getInputStream();
            } else {
                ins = conne.getErrorStream();
            }
        } catch (SSLException e) {
            e.printStackTrace();
            return new byte[0];
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int len;
        while ((len = ins.read(buff)) != -1) {
            baos.write(buff, 0, len);
        }
        byte[] bytes = baos.toByteArray();
        ins.close();
        return bytes;
    }

    private static String getBoundary() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 32; ++i) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".length())));
        }
        return sb.toString();
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }


    /**
     * 人脸识别
     * @param imgBase64
     */
    public static R faceDetect(String imgBase64) {
        HashMap<String, String> map = new HashMap<>();
        HashMap<String, byte[]> byteMap = new HashMap<>();
        map.put("api_key", API_KEY); // 调用此API的API Key
        map.put("api_secret", API_SECRET); // 调用此API的API Secret
        map.put("return_landmark", "1"); // 是否检测并返回人脸关键点，1 表示返回 83 个人脸关键点
        map.put("return_attributes", "gender,age,smiling,headpose,facequality,blur,eyestatus,emotion,ethnicity,beauty,mouthstatus,eyegaze,skinstatus"); // 是否检测并返回根据人脸特征判断出的年龄、性别、情绪等属性
        map.put("image_base64", imgBase64);
//        File file = new File(imgBase64);
        byte[] buff = getBytesFromFile(FileUtil.file(imgBase64));
        byteMap.put("image_file", buff);
        R dataResp = new R();

        String respString = "";
        try {
            byte[] respByte = post(FACE_API_DETECT, map, byteMap);

            respString = new String(respByte);
            getFaceId(respString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!StringUtils.isEmpty(respString)) {
            System.out.println("脸部识别响应：" + respString);
            JSONObject json = JSON.parseObject(respString);
            // 被检测出的人脸数组
            JSONArray faces = json.getJSONArray("faces");
            if (faces.size() > 0) {
                // 默认取识别出的第一张人脸
                JSONObject face = (JSONObject) faces.get(0);

                System.out.println(face.toString());
                // 获取 facequality 字段，用于判断图片质量是否可以用于后续的人脸对比
                JSONObject fq = face.getJSONObject("attributes").getJSONObject("facequality");

                if (validateFaceQuality(fq)) {
                    R.ok(R.Code.SUCCESS);
                    R.ok("录入成功");
                    // 返回识别的脸部 json 数据，用于后续操作
                    R.ok(face);
                } else {
                    R.ok(R.Code.ERROR);
                    R.error("请端正姿势");
                    System.out.println("请端正姿势");
                }

            } else {
                R.ok(R.Code.ERROR);
                R.error("识别不到人脸");
                System.out.println("识别不到人脸");
            }
        }

        return dataResp;
    }

    /**
     * 人脸对比
     * @return
     */
    public static String faceCompare(String faceToken1, String faceToken2) {

        HashMap<String, String> map = new HashMap<>();
        HashMap<String, byte[]> byteMap = new HashMap<>();
        map.put("api_key", API_KEY);
        map.put("api_secret", API_SECRET);
        map.put("face_token1", faceToken1); // 用于对比的第一张 base64 编码图片
        map.put("face_token2", faceToken2); // 用于对比的第二张 base64 编码图片
        String msg = null;

        String respString = "";
        try {
            byte[] respByte = post(FACE_API_COMPARE, map, null);

            respString = new String(respByte);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!StringUtils.isEmpty(respString)) {
            System.out.println("脸部对比响应：" + respString);

            JSONObject json = JSON.parseObject(respString);
            if (validateFaceConfidence(json)) {
                msg ="SUCCESS";
                R.ok("刷脸对比成功");
                R.ok(json);
            } else {
                R.ok(R.Code.ERROR);
                R.error("刷脸失败，不是同一个人");
                R.ok(json);
            }
        }

        return msg;
    }

    /**
     * 校验人脸质量
     * @return
     */
    public static boolean validateFaceQuality(JSONObject fq) {
        if (fq != null) {
            // value 人脸的质量判断的分数，是一个浮点数
            double value = fq.getDouble("value");
            // threshold 表示人脸质量基本合格的一个阈值，超过该阈值的人脸适合用于人脸比对
            double threshold = fq.getDouble("threshold");

            return value > threshold;
        }

        return false;
    }

    /**
     * 校验置信度，也就是判断是不是同一个人
     * @return
     */
    public static boolean validateFaceConfidence(JSONObject json) {
        if (json != null) {
            // 获取比对结果置信值
            double confidence = json.getDouble("confidence");
            // 获取误识率为十万分之一的置信度阈值
            double threshold1E5 = json.getJSONObject("thresholds").getDouble("1e-5");

            // 如果置信值超过“十万分之一”阈值，则是同一个人的几率非常高
            return confidence > threshold1E5;
        }
        return false;
    }


    public static byte[] getBytesFromFile(File f) {
        if (f == null) {
            return null;
        }
        try {
            FileInputStream stream = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = stream.read(b)) != -1)
                out.write(b, 0, n);
            stream.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    public static String getFaceId(String respString){
        JSONObject jsonObject=JSONObject.parseObject (respString);

        int num=jsonObject.getIntValue ("face_num");
        if (num==1) {
            JSONArray array = (JSONArray) jsonObject.get("faces");
            JSONObject face = (JSONObject) array.get(0);
            String faceToken = face.getString("face_token");
            faceId = faceToken;
            return faceId;
        }
        return null;
    }
}
