package com.sweep.project.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.sweep.project.alarm.batch.AlarmMessageDto;
import com.sweep.project.fcm.service.FcmSendService;
import com.sweep.project.fcm.service.FcmTokenService;
import com.sweep.project.redis.RedisMessageDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RabbitMqManager {
    private final static String dLQName="DLQ";
    private final static String dLXName="DLX";
    private final CachingConnectionFactory connectionFactory;
    private final static String alarmQueueName="alarm";
    private final static String alarmExchangeName="alarmExchange";
    private final RabbitAdmin rabbitAdmin;
    private final ObjectMapper objectMapper;
    private final RetryOperationsInterceptor batchRetryOperationsInterceptor;
    private final FcmSendService fcmSendService;
    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> map
            =new ConcurrentHashMap<>();

    @PostConstruct
    public void setting(){
        //createBasicSetting();
        alarmSetting();
    }
    public void alarmSetting(){
        final String title = "아내일점심은 뭐먹을까";
        final String body = "내일 점심은 가지무침이다.";
        Queue actionQueue=createAlramQueue();
        Binding binding= BindingBuilder.bind(actionQueue)
                .to(createAlarmExchange())
                .with(alarmQueueName);
        rabbitAdmin.declareBinding(binding);
        rabbitAdmin.declareBinding(binding);
        ChannelAwareBatchMessageListener messageListener= (ChannelAwareBatchMessageListener) (messages, channel)
                -> {
            try {
                List<Message> messages1 = new ArrayList<>();    // messages1 -> Firebase에 실제로 보낼 메시지들
                List<RedisMessageDto> metadata = new ArrayList<>(); // metadata -> log 저장에 필요한 정보들 ex) memberId, token
                messages.stream().forEach(x -> {
                    try {
                        com.sweep.project.redis.RedisMessageDto redisMessageDto= objectMapper.readValue(x.getBody(), RedisMessageDto.class);
                        Message message = Message.builder()
                                .setToken(redisMessageDto.getToken())
                                .setNotification(Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        /*
                                        * 나중에 알람타입에 따라서 보내는 알람도 변경--> fix의경우 notification이아닌 다른 형태의 알람으로
                                        * */
                                        .build())
                                .build();
                        messages1.add(message);
                        metadata.add(redisMessageDto);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                fcmSendService.bulkPushWithLog(messages1, metadata, title, body);
            }
            catch (Exception e){
                throw new RuntimeException(e.getMessage());
            }
        };

        SimpleMessageListenerContainer container=
                new SimpleMessageListenerContainer(connectionFactory);
        container.setMessageListener(messageListener);
        container.addQueueNames(alarmQueueName);
        container.setPrefetchCount(30);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setAdviceChain(batchRetryOperationsInterceptor);
        container.setConcurrentConsumers(3);
        container.setConsumerBatchEnabled(true);
        container.setReceiveTimeout(2000L);
        container.setBatchSize(15);
        container.setRecoveryInterval(30000L);
        container.start();
        map.put(alarmQueueName,container);



    }

    /*public void createBasicSetting(){
        Queue dlq=createDlq();
        Binding binding=BindingBuilder.bind(dlq)
                .to(createDlx())
                .with(dLQName);
        rabbitAdmin.declareBinding(binding);
        ChannelAwareBatchMessageListener messageListener= (ChannelAwareBatchMessageListener) (messages, channel)
                -> {
            try {
                List<Message> messages1 = new ArrayList<>();
                messages.stream().forEach(x -> {
                    log.info("메시지가 최종적으로 실패:{}",)
                });
                fcmSendService.bulkPush(messages1);
            }
            catch (Exception e){
                throw new RuntimeException(e.getMessage());
            }
        };

        SimpleMessageListenerContainer container=
                new SimpleMessageListenerContainer(connectionFactory);
        container.setMessageListener(messageListener);
        container.addQueueNames(dLQName);
        container.setPrefetchCount(30);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setAdviceChain(batchRetryOperationsInterceptor());
        container.setConcurrentConsumers(1);
        container.setConsumerBatchEnabled(true);
        container.setReceiveTimeout(2000L);
        container.setBatchSize(15);
        container.setRecoveryInterval(30000L);
        container.start();
        map.put(dLQName,container);
    }*/
    private Queue createAlramQueue(){
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dLXName);           // 기본 exchange 사용
        args.put("x-dead-letter-routing-key", dLQName);   // DLQ 이름으로 라우팅
        Queue queue=new Queue(alarmQueueName,true,false,false,args);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }
    private DirectExchange createAlarmExchange(){
        org.springframework.amqp.core.DirectExchange directExchange =
                new DirectExchange(alarmExchangeName,true,false);
        rabbitAdmin.declareExchange(directExchange);
        return directExchange;
    }
    public Queue createDlq(){
        Queue queue=new Queue(dLQName,true,false,false);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }
    public DirectExchange createDlx(){
        DirectExchange directExchange=
                new DirectExchange(dLXName,true,false);
        rabbitAdmin.declareExchange(directExchange);
        return directExchange;
    }
}
