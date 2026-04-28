package com.sweep.project.route.batch;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.Message;
import com.sweep.project.fcm.service.FcmSendService;
import com.sweep.project.route.domain.RouteTicketRepo;
import com.sweep.project.route.dto.NeedCheckAlertDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * 경로 데이터가 갱신된 알람의 needCheck 를 true 로 표시하고,
 * 해당 멤버의 모든 기기에 FCM silent push 를 발송한다.
 */
@RequiredArgsConstructor
public class AlarmNeedCheckWriter implements ItemWriter<Long> {

    private final RouteTicketRepo routeTicketRepo;
    private final FcmSendService fcmSendService;

    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        routeTicketRepo.updateNeedCheckBatch(chunk.getItems());

        List<NeedCheckAlertDto> alerts = routeTicketRepo.getFcmTokenFromAlarm(chunk.getItems());

        List<Message> messages = alerts.stream()
                .flatMap(x -> x.getFcmTokens().stream().map(token ->
                        Message.builder()
                                .putData("alarmId", x.getAlarmId().toString())
                                .setToken(token)
                                .setApnsConfig(ApnsConfig.builder()
                                        .setAps(Aps.builder()
                                                .setContentAvailable(true)
                                                .build())
                                        .build())
                                .build()
                ))
                .toList();
        fcmSendService.bulkPush(messages);
    }
}
