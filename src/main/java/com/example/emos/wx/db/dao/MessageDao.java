package com.example.emos.wx.db.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository
public class MessageDao {
    private MongoTemplate mongoTemplate;

    public String insert(MessageEntity entity){
        Date sendTime = entity.getSendTime();
//        北京时间转换成格林时间
        sendTime = DateUtil.offset(sendTime, DateField.HOUR, 8);
        entity.setSendTime(sendTime);
        entity=mongoTemplate.save(entity);
        return entity.get_id();
    }

//    按照分页查询消息
    public List<HashMap> searchMessageByPage(int userId, long start, int length){
        JSONObject json=new JSONObject();
        json.set("$toString","$_id");
//    MongoDB集合查询java写法，不是通过new出来的而是Aggregation.newAggregation();
        Aggregation aggregation=Aggregation.newAggregation(
//                声明临时变量
                Aggregation.addFields().addField("id").withValue(json).build(),
//                集合联合查询
                Aggregation.lookup("message_ref","id","messageId","ref"),
//                设置where条件
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)),
//                排序函数，按照发送时间进行降序排列
                Aggregation.sort(Sort.by(Sort.Direction.DESC,"sendTime")),
                Aggregation.skip(start),
                Aggregation.limit(length)
        );
//        进行联合查询
        AggregationResults<HashMap> results=mongoTemplate.aggregate(aggregation,"message",HashMap.class);
//         把结果里的数据提取成list
        List<HashMap> list = results.getMappedResults();
        list.forEach(one->{
//            ref取出来后对应的是一个list，因为在做集合连接的时候可能是一对多的，这个引用的字段可能对应多条记录得到的数据可能并不是单一数据
            List<MessageRefEntity> refList= (List<MessageRefEntity>) one.get("ref");
//            从list取出数据，这个链接是一对一的连接通过refList.get(0);
            MessageRefEntity entity=refList.get(0);
            boolean readFlag=entity.getReadFlag();
            String refId=entity.get_id();
            one.put("readFlag",readFlag);
            one.put("refId",refId);
            one.remove("ref");
            one.remove("_id");
            Date sendTime= (Date) one.get("sendTime");
            sendTime=DateUtil.offset(sendTime,DateField.HOUR,-8);

            String today=DateUtil.today();
            if(today.equals(DateUtil.date(sendTime).toDateStr())){
                one.put("sendTime",DateUtil.format(sendTime,"HH:mm"));
            }
            else{
                one.put("sendTime",DateUtil.format(sendTime,"yyyy/MM/dd"));
            }
        });
        return list;
    }

    public HashMap searchMessageById(String id){
        HashMap map=mongoTemplate.findById(id,HashMap.class,"message");
        Date sendTime= (Date) map.get("sendTime");
        sendTime=DateUtil.offset(sendTime,DateField.HOUR,-8);
        map.replace("sendTime",DateUtil.format(sendTime,"yyyy-MM-dd HH:mm"));
        return map;
    }
}
