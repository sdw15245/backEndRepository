package com.sweep.project.config;

import com.rabbitmq.client.ShutdownSignalException;
import com.sweep.project.discord.DiscordChannel;
import com.sweep.project.discord.DiscordEvent;
import com.sweep.project.discord.service.DiscordSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitMqConfig {
    @Value("${spring.rabbitmq.host}")
    private String hostname;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    private AtomicBoolean disconnected = new AtomicBoolean(false);

    private final DiscordSendService discordSendService;


    @Bean
    public CachingConnectionFactory connectionFactory(){
        CachingConnectionFactory connectionFactory=new CachingConnectionFactory(hostname,port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setRequestedHeartBeat(60);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.setChannelCacheSize(15); //캐시해서 미리 저장해둘 channel size설정값
        connectionFactory.addConnectionListener(new ConnectionListener() {
            @Override
            public void onCreate(Connection connection) {
                // 연결 성공 (재연결)
                if(disconnected.compareAndSet(true, false)){
                    log.info("RabbitMQ 연결 성공");
                        discordSendService.sendAlert(DiscordChannel.MQ,
                                DiscordEvent.success("mq 재연결 성공",
                                        "재연결 성공", LocalDateTime.now()));
                        disconnected.compareAndSet(true, false);
                    }
            }
            @Override
            public void onShutDown(ShutdownSignalException signal) {
                // 연결 끊김 감지
                if(disconnected.compareAndSet(false, true)) {
                    log.info("RabbitMQ 연결 끊김: {}", signal.getMessage());
                    discordSendService.sendAlert(DiscordChannel.MQ,
                            DiscordEvent.success("mq 연결 끊김 감지", "재시도 시작", LocalDateTime.now()));
                }
            }
        });
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(){
        return new RabbitAdmin(connectionFactory());
    }

    /*
    * 이거 주입받아서 메시지 전송
    * ex)
    * */

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate=new RabbitTemplate(connectionFactory());
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setConfirmCallback((data,ack,cause)->{
            if(!ack){
                log.info("메시지 전송 실패:{}-{}",data.getId(),cause);
            }
        });
        rabbitTemplate.setReturnsCallback((returnedMessage)->{
            log.info("라우팅 실패된 메시지:{}-{}",
                    returnedMessage.getExchange(),returnedMessage.getRoutingKey());
        });

        rabbitTemplate.setRetryTemplate(RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000L,2.0,8000L)
                .retryOn(AmqpConnectException.class)  // TCP 연결 실패
                .retryOn(AmqpIOException.class)       // IO 오류
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
                        log.info("retry 성공:{}",context.getRetryCount());
                    }
                    @Override
                    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        log.info("retry 실패:{}",context.getRetryCount(),context.getLastThrowable().getMessage());
                    }
                })
                .build());

        rabbitTemplate.setRecoveryCallback((context -> {
            log.info("리커버리 context 작동:{}", context.getLastThrowable().getMessage());
            return null;
        }
        ));
        return rabbitTemplate;
    }

    @Bean(name ="batchRetryOperationsInterceptor")
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
