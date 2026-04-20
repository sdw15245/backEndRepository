package com.sweep.project.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.sweep.project.alarm.batch.AlarmMessageDto;
import com.sweep.project.fcm.service.FcmSendService;
import com.sweep.project.fcm.service.FcmTokenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.context.annotation.Bean;
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

@org.springframework.context.annotation.Configuration
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
    private final FcmSendService fcmSendService;
    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> map
            =new ConcurrentHashMap<>();

    @PostConstruct
    public void setting(){
        createBasicSetting();
        alarmSetting();
    }
    public void alarmSetting(){
        Queue actionQueue=createAlramQueue();
        Binding binding= BindingBuilder.bind(actionQueue)
                .to(createAlarmExchange())
                .with(alarmQueueName);
        rabbitAdmin.declareBinding(binding);
    }

    public void createBasicSetting(){
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
                    try {
                        AlarmMessageDto alarmMessageDto = objectMapper.readValue(x.getBody(), AlarmMessageDto.class);
                        alarmMessageDto.getFcmTokens().stream().forEach(y -> {
                            Message message = Message.builder()
                                    .setToken(y)
                                    .setNotification(Notification.builder()
                                            .setTitle("준비알람")
                                            .setBody("준비알람")
                                            .build())
                                    .build();
                            messages1.add(message);
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
        container.setConcurrentConsumers(3);
        container.setConsumerBatchEnabled(true);
        container.setReceiveTimeout(2000L);
        container.setBatchSize(15);
        container.setRecoveryInterval(30000L);
        container.start();
        map.put(dLQName,container);
    }
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


    @Bean
    public RetryOperationsInterceptor batchRetryOperationsInterceptor(){
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(3.0);
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(new SimpleRetryPolicy(2))
                .backOffPolicy(backOffPolicy)
                .recoverer((message, cause) -> {
                    log.error("재시도 고갈 - reason:{}", cause.getMessage());
                    return null;
                })
                .build();
    }
}
