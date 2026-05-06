package com.sweep.project.redis;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
/**
 * keyexpiration eventmessgelistener를 직접 상속받응 애라서 redis에 특별히 설정파일을 건들 이유도없고
 * 자동으로 redis container에 등록됨.
 *
 * */
@Component
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private final String prefixKey="alarm-";
    private static final String EXCHANGE    = "alarmExchange";
    private static final String ROUTING_KEY = "alarm";
    private final RabbitTemplate rabbitTemplate;

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer,
                                      RabbitTemplate rabbitTemplate) {
        super(listenerContainer);
        this.rabbitTemplate=rabbitTemplate;
    }
    // redis key는 alarm-memberId-alarmId--type-token-idx(준비알람의 경우 들어가는값-->1개의 알람 id에 대해서 여러개이므로) 으로 구성될것임.
    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("expired key:{}",message.toString());
        if(message.toString().startsWith(prefixKey)){
           /* String [] keys=message.toString().split("-");
            com.sweep.project.redis.RedisMessageDto redisMessageDto = new RedisMessageDto(
                    keys[1],keys[2],keys[3], keys[4] ); */
            String key = message.toString();
            String[] parts = key.split("-");

            String memberId = parts[1];
            String alarmId = parts[2];
            String alarmType = parts[3];

            int tokenEndExclusive = parts.length;
            if ("prepare".equals(alarmType) && parts.length > 5) {
                tokenEndExclusive = parts.length - 1; // 마지막 prepare index 제거
            }

            String token = String.join(
                    "-",
                    java.util.Arrays.copyOfRange(parts, 4, tokenEndExclusive)
            );

            RedisMessageDto redisMessageDto = new RedisMessageDto(
                    memberId, alarmId, alarmType, token
            );
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY,redisMessageDto, msg -> {
                msg.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return msg;
            });

        }
    }
}
