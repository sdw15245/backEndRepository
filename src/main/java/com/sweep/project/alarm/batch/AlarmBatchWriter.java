package com.sweep.project.alarm.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.fcm.domain.FcmToken;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import com.sweep.project.route.OdsayRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알람 배치 라이터.
 *
 * <h3>처리 흐름</h3>
 * <pre>
 * 1. 청크 내 AlarmBatchDto 를 memberId 기준으로 그룹핑
 * 2. 멤버별 FCM 토큰 조회
 * 3. 각 Alarm 에 대해
 *    a) routeData → totalTime(분) 파싱
 *    b) departureTime  = arrivalTime - totalTime
 *    c) 출발 알람  1개 : triggerTime = departureTime
 *    d) 준비 알람  N개 : prepareStartTime = departureTime - prepareTime
 *                        N = prepareTime / interval  (정수 나눗셈)
 *                        triggerTime_i = prepareStartTime + i * interval  (i=0..N-1)
 * 4. 각 메시지에 TTL 설정
 *    TTL(ms) = max(0, triggerTime - schedulerRunAt)
 *    → 만료되면 알람 큐의 DLX 설정에 의해 DLQ 로 이동
 * 5. AMQP messageId = "{alarmId}_{memberId}_{type}_{index}"
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class AlarmBatchWriter implements ItemWriter<AlarmBatchDto> {

    private static final String EXCHANGE    = "alarmExchange";
    private static final String ROUTING_KEY = "alarm";

    private final RabbitTemplate rabbitTemplate;
    private final FcmTokenRepository fcmTokenRepository;
    private final ObjectMapper objectMapper;

    /** 스케쥴러 실행 시각 — TTL 계산 기준 */
    private final LocalDateTime schedulerRunAt;

    // ── ItemWriter ────────────────────────────────────────────────────────────

    @Override
    public void write(Chunk<? extends AlarmBatchDto> chunk) {
        // 1. memberId 기준 그룹핑
        Map<Long, List<AlarmBatchDto>> grouped = chunk.getItems().stream()
                .collect(Collectors.groupingBy(AlarmBatchDto::getMemberId));

        for (Map.Entry<Long, List<AlarmBatchDto>> entry : grouped.entrySet()) {
            Long memberId = entry.getKey();

            // 2. FCM 토큰 조회
            List<String> tokens = fcmTokenRepository.findAllByMemberId(memberId)
                    .stream()
                    .map(FcmToken::getToken)
                    .collect(Collectors.toList());

            if (tokens.isEmpty()) {
                log.debug("[AlarmWriter] FCM 토큰 없음 — memberId={} 스킵", memberId);
                continue;
            }

            for (AlarmBatchDto dto : entry.getValue()) {
                processAlarm(dto, memberId, tokens);
            }
        }

        log.info("[AlarmWriter] 청크 처리 완료 — 알람 수={}", chunk.size());
    }

    // ── 알람 계산 및 발행 ─────────────────────────────────────────────────────

    private void processAlarm(AlarmBatchDto dto, Long memberId, List<String> tokens) {

        if (dto.getRouteData() == null) {
            log.warn("[AlarmWriter] routeData null — alarmId={} 스킵", dto.getAlarmId());
            return;
        }
        /*
        * 이거같은 경우에는 알람 생성시 ARRIVALTIME을 반드시 입력받게 할거라서 고려할 문제는 아닌거같은대 일단은 냅두도록 하겠음.
        * */
        if (dto.getArrivalTime() == null) {
            log.warn("[AlarmWriter] arrivalTime null — alarmId={} 스킵", dto.getAlarmId());
            return;
        }

        // 3a. routeData 파싱 → totalTime (분)
        int totalTime;
        try {
            OdsayRouteResponse response = objectMapper.readValue(dto.getRouteData(), OdsayRouteResponse.class);
            totalTime = response.getResult().getPath().get(0).getInfo().getTotalTime();
        } catch (Exception e) {
            log.warn("[AlarmWriter] routeData 파싱 실패 — alarmId={} : {}", dto.getAlarmId(), e.getMessage());
            return;
        }

        // 3b. 출발 시각
        LocalDateTime departureTime = dto.getArrivalTime().minusMinutes(totalTime);

        // 3c. 출발 알람 1개
        publish(dto, memberId, tokens, AlarmType.DEPARTURE, departureTime, 0);

        // 3d. 준비 알람 N개
        if (dto.getPrepareTime() != null && dto.getInterval() != null && dto.getInterval() > 0) {
            LocalDateTime prepareStart = departureTime.minusMinutes(dto.getPrepareTime());
            int count = dto.getPrepareTime() / dto.getInterval();

            for (int i = 0; i < count; i++) {
                LocalDateTime triggerTime = prepareStart.plusMinutes((long) i * dto.getInterval());
                publish(dto, memberId, tokens, AlarmType.PREPARE, triggerTime, i);
            }
        }
    }

    // ── RabbitMQ 발행 ─────────────────────────────────────────────────────────

    private void publish(AlarmBatchDto dto, Long memberId, List<String> tokens,
                         AlarmType type, LocalDateTime triggerTime, int index) {
        String typeName  = type == AlarmType.DEPARTURE ? "departure" : "prepare";
        String messageId = dto.getAlarmId() + "_" + memberId + "_" + typeName + "_" + index;

        // TTL: 스케쥴러 실행 시각 → triggerTime 까지의 밀리초
        long ttlMillis = Math.max(0L, Duration.between(schedulerRunAt, triggerTime).toMillis());

        AlarmMessageDto message = new AlarmMessageDto(
                messageId, dto.getAlarmId(), memberId, tokens, type, triggerTime);

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message, msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(ttlMillis));
            msg.getMessageProperties().setMessageId(messageId);
            msg.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return msg;
        });

        log.debug("[AlarmWriter] 발행 — messageId={} triggerTime={} ttl={}ms",
                messageId, triggerTime, ttlMillis);
    }
}
