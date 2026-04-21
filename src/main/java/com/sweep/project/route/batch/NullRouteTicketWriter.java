package com.sweep.project.route.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.Message;
import com.sweep.project.alarm.batch.AlarmBatchDto;
import com.sweep.project.alarm.batch.AlarmMessageDto;
import com.sweep.project.alarm.batch.AlarmType;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import com.sweep.project.fcm.service.FcmSendService;
import com.sweep.project.route.domain.RouteTicketRepo;
import com.sweep.project.route.dto.NeedCheckAlertDto;
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
    private final FcmSendService fcmSendService;

    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        routeTicketRepo.updateNeedCheckBatch(chunk.getItems());

        List<NeedCheckAlertDto> needCheckAlertDtos=routeTicketRepo.getFcmTokenFromRouteTicket(chunk.getItems());

        List<Message> messages=needCheckAlertDtos.stream().map(x->{
            return Message.builder()
                    .putData("routeTicketId",x.getRouteTicketId().toString())
                    .setToken(x.getFcmToken())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setContentAvailable(true)
                                    .build())
                            .build())
                    .build();
        }).toList();
        fcmSendService.bulkPush(messages);
    }

}
