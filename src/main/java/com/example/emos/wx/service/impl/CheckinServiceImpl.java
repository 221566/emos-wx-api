package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.util.FaceHelper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SystemConstants constants;

    @Resource
    private TbHolidaysDao holidaysDao;

    @Resource
    private TbWorkdayDao workdayDao;

    @Resource
    private TbCheckinDao checkinDao;

    @Resource
    private TbFaceModelDao faceModelDao;

    @Resource
    private TbCityDao cityDao;

    @Resource
    private TbUserDao userDao;


    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.code}")
    private String code;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null?true:false;
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null?true:false;
        String type = "工作日";
//        判断是不是周末
        if (DateUtil.date().isWeekend()){
            type="节假日";
        }
        if (bool_1){
            type = "节假日";
        }else if (bool_2){
            type = "工作日";
        }

        if (type.equals("节假日")){
            return "节假日不需要考勤";
        }else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStartTime = DateUtil.parse(start);
            DateTime attendanceEndTime = DateUtil.parse(end);
            if (now.isBefore(attendanceStartTime)){
                return "没到上班考勤时间";
            }else if (now.isAfter(attendanceEndTime)){
                return "超过了上班考勤结束时间";
            }else {
                HashMap map = new HashMap();
                map.put("userId",userId);
                map.put("date",date);
                map.put("start",start);
                map.put("end",end);
                boolean bool = checkinDao.haveCheckin(map) != null?true:false;
                return bool?"今日已经考勤，不用重复考勤":"可以考勤";
            }
        }
    }
    @Override
    public void checkin(HashMap param) {
        Date d1=DateUtil.date();
        Date d2=DateUtil.parse(DateUtil.today()+" "+constants.attendanceTime);
        Date d3=DateUtil.parse(DateUtil.today()+" "+constants.attendanceEndTime);
        int status=1;
        if(d1.compareTo(d2)<=0){
            status=1;
        }
        else if(d1.compareTo(d2)>0&&d1.compareTo(d3)<0){
            status=2;
        }
        else{
            throw new EmosException("超出考勤时间段，无法考勤");
        }
        int userId= (Integer) param.get("userId");
        String faceModel=faceModelDao.searchFaceModel(userId);
        if(faceModel==null){
            throw new EmosException("不存在人脸模型");
        }
        else{
            String path=(String)param.get("path");
             FaceHelper.faceDetect(path);
            String s = FaceHelper.faceCompare(FaceHelper.faceId, faceModel);
            if(s.equals("SUCCESS")){
                //查询疫情风险等级
                int risk=1;
                String city= (String) param.get("city");
                String district= (String) param.get("district");
                String address= (String) param.get("address");
                String country= (String) param.get("country");
                String province= (String) param.get("province");
                if(!StrUtil.isBlank(city)&&!StrUtil.isBlank(district)){
                    String code=cityDao.searchCode(city);
                    try{
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                        Document document=Jsoup.connect(url).get();
                        Elements elements=document.getElementsByClass("list-content");
                        if(elements.size()>0){
                            Element element=elements.get(0);
                            String result=element.select("p:last-child").text();
//                            result="高风险";
                            if("高风险".equals(result)){
                                risk=3;
                                //发送告警邮件
//                                HashMap<String,String> map=userDao.searchNameAndDept(userId);
//                                String name = map.get("name");
//                                String deptName = map.get("dept_name");
//                                deptName = deptName != null ? deptName : "";
//                                SimpleMailMessage message=new SimpleMailMessage();
//                                message.setTo(hrEmail);
//                                message.setSubject("员工" + name + "身处高风险疫情地区警告");
//                                message.setText(deptName + "员工" + name + "，" + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address + "，属于新冠疫情高风险地区，请及时与该员工联系，核实情况！");
//                                emailTask.sendAsync(message);
                            }
                            else if("中风险".equals(result)){
                                risk=2;
                            }
                        }
                    }catch (Exception e){
                        log.error("执行异常",e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
                //保存签到记录
                TbCheckin entity=new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
        }
    }

    @Override
    public void createFaceModel(int userId, String path) {
            FaceHelper.faceDetect(path);
            TbFaceModel entity=new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(FaceHelper.faceId);
            faceModelDao.insert(entity);
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map=checkinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days=checkinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList=checkinDao.searchWeekCheckin(param);
        ArrayList holidaysList=holidaysDao.searchHolidaysInRange(param);
        ArrayList workdayList=workdayDao.searchWorkdayInRange(param);
        DateTime startDate=DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate=DateUtil.parseDate(param.get("endDate").toString());
        DateRange range=DateUtil.range(startDate,endDate, DateField.DAY_OF_MONTH);
        ArrayList<HashMap> list=new ArrayList<>();
        range.forEach(one->{
            String date=one.toString("yyyy-MM-dd");
            String type="工作日";
            if(one.isWeekend()){
                type="节假日";
            }
            if(holidaysList!=null&&holidaysList.contains(date)){
                type="节假日";
            }
            else if(workdayList!=null&&workdayList.contains(date)){
                type="工作日";
            }
            String status="";
            if(type.equals("工作日")&&DateUtil.compare(one,DateUtil.date())<=0){
                status="缺勤";
                boolean flag=false;
                for (HashMap<String,String> map:checkinList){
                    if(map.containsValue(date)){
                        status=map.get("status");
                        flag=true;
                        break;
                    }
                }
                DateTime endTime=DateUtil.parse(DateUtil.today()+" "+constants.attendanceEndTime);
                String today=DateUtil.today();
                if(date.equals(today)&&DateUtil.date().isBefore(endTime)&&flag==false){
                    status="";
                }
            }
            HashMap map=new HashMap();
            map.put("date",date);
            map.put("status",status);
            map.put("type",type);
            map.put("day",one.dayOfWeekEnum().toChinese("周"));
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }
}
