package com.sweep.project.route.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.alarm.batch.AlarmBatchDto;
import com.sweep.project.alarm.batch.AlarmMessageDto;
import com.sweep.project.alarm.batch.AlarmType;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class NullRouteTicketWriter implements ItemWriter<Long> {

    private final RouteTicketRepo routeTicketRepo;

    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        routeTicketRepo.updateNeedCheckBatch(chunk.getItems());
        /*
        * postgresql의 데비지움 하고 연결해서 cdc기반으로 needcheck 가 update된걸 캐치해서 rabbitmq에 넘기는걸로 생각을 해봐야겠는대
        * */
    }

}
