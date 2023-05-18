package com.example.emos.wx.task;


import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MessageTask {
    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private MessageService messageService;

    public void send(String topic, MessageEntity entity) {
        String id = messageService.insertMessage(entity);
        /**
         * 连接： Connection connection = factory.newConnection();
         * 通道： Channel channel = connection.createChannel();
         * 这段代码是使用RabbitMQ的Java客户端创建一个连接和一个通道。
         * RabbitMQ是一个开源的消息代理，它实现了高级消息队列协议（AMQP）标准，
         * 并提供了可靠的消息传递机制。在这段代码中，首先通过ConnectionFactory创建一个连接，
         * 然后通过Connection创建一个通道，这个通道可以用来发送和接收消息。在RabbitMQ中，
         * 生产者将消息发送到交换机，交换机根据指定的路由键将消息路由到一个或多个队列中，消费者从队列中获取消息并进行处理
         */
//        由于需要创建连接所以我用try{}语句块来执行，执行完try语句块后会自动关闭连接
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
//            连接队列，不存在就会创建这个队列
            channel.queueDeclare(topic, true, false, false, null);
            HashMap map = new HashMap();
            map.put("messageId", id);
            /**
             * 这段代码是使用 RabbitMQ 的 Java 客户端库中的 AMQP.BasicProperties 类创建一个 AMQP 消息的属性对象，
             * 并将其中的 headers 属性设置为指定的 Map 对象。headers 属性是一个键值对的 Map，
             * 可以用来携带一些自定义的消息头信息，比如消息的类型、版本号、编码方式等。
             * 这些信息可以帮助消费者更好地处理消息。在创建 AMQP 消息时，可以将这个属性对象作为参数传递给消息的发送方法，
             * 比如 channel.basicPublish() 方法。
             */
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(map).build();
            channel.basicPublish("", topic, properties, entity.getMsg().getBytes());
            log.debug("消息发送成功");
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("向MQ发送消息失败");
        }
    }

    @Async
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }

//    接收消息的代码
    public int receive(String topic) {
        int i = 0;
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
            channel.queueDeclare(topic, true, false, false, null);
//            从队列里接收数据，因为不知道存放了多少条消息，因为不知道需要多少次可以接收完，需要写个死循环来不断接收数据，等到接收完用break退出
            while (true) {
//                使用channel.basicGet（）来接收数据
                GetResponse response = channel.basicGet(topic, false);
                if (response != null) {
                    AMQP.BasicProperties properties = response.getProps();
                    Map<String, Object> map = properties.getHeaders();
                    String messageId = map.get("messageId").toString();
//                    获取消息正文response.getBody();
                    byte[] body = response.getBody();
//                    把byte转成String
                    String message = new String(body);
                    log.debug("从RabbitMQ接收的消息：" + message);

                    MessageRefEntity entity = new MessageRefEntity();
                    entity.setMessageId(messageId);
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false);
                    entity.setLastFlag(true);
                    messageService.insertRef(entity);
//                    返回ACK应答的第一个参数
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
//                    返回ACK应答，表示消费者成功接收到消息
                    channel.basicAck(deliveryTag, false);
//                    接收的消息增加
                    i++;
                }
                else {
//                   没有消息可以接收后就可以退出循环了
                    break;
                }
            }
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("接收消息失败");
        }
        return i;
    }

    @Async
    public int receiveAsync(String topic) {
        return receive(topic);
    }


    public void deleteQueue(String topic){
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
            channel.queueDelete(topic);
            log.debug("消息队列成功删除");
        }catch (Exception e) {
            log.error("删除队列失败", e);
            throw new EmosException("删除队列失败");
        }
    }

    @Async
    public void deleteQueueAsync(String topic){
        deleteQueue(topic);
    }
}
