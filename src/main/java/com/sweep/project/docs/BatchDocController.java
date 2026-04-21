package com.sweep.project.docs;

import com.sweep.project.docs.fcm.FcmAlarmNotificationSchema;
import com.sweep.project.docs.fcm.FcmSilentPushSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * FCM으로 실제 전송되는 Message 객체 구조 참고용 Swagger 문서 컨트롤러.
 * 모든 엔드포인트는 501 Not Implemented를 반환합니다.
 */
@Tag(
    name = "[참고] FCM 전송 메시지 구조",
    description = """
        **⚠️ 실제 API가 아닙니다. 모든 엔드포인트는 501 Not Implemented를 반환합니다.**

        FCM으로 실제 전송되는 `com.google.firebase.messaging.Message` 객체 구조를 확인하기 위한 참고 문서입니다.

        ---

        ### 메시지 유형 요약

        | 알람 유형 | 발송 위치 | Notification | Data | APNs silent |
        |-----------|-----------|:---:|:---:|:---:|
        | PREPARE / DEPARTURE | RabbitMqManager | ✅ | ❌ | ❌ |
        | FIX (경로 소멸) | NullRouteTicketWriter | ❌ | ✅ | ✅ |

        ### 발송 흐름

        ```
        [PREPARE / DEPARTURE]
        Redis TTL 만료
          → RedisKeyExpirationListener
          → RabbitMQ 'alarm' 큐 발행
          → RabbitMqManager 컨슈머 (batch, prefetch=30, batchSize=15)
          → FCM Notification 메시지 bulkPush

        [FIX — 경로 소멸]
        routeUpdateJob Step2 (NullRouteTicketWriter)
          → RouteTicket.needCheck = true 갱신
          → FCM Data-only Silent Push bulkPush
        ```
        """
)
@RestController
@RequestMapping("/docs/fcm")
public class BatchDocController {

    private static final String NOT_IMPL = "참고용 문서입니다. 실제 동작하지 않습니다.";

    @Operation(
        summary = "PREPARE / DEPARTURE 알람 — Notification 메시지",
        description = """
            **발송 위치**: `RabbitMqManager.alarmSetting()` — RabbitMQ `alarm` 큐 컨슈머

            **트리거**: Redis TTL 만료 → `RedisKeyExpirationListener` → RabbitMQ 발행 → 컨슈머 수신

            ---

            #### 메시지 특성
            - **Notification 포함**: ✅ YES — 시스템 트레이(알림 배너)에 표시됨
            - **Data 페이로드**: ❌ 없음
            - **APNs silent**: ❌ 없음

            #### 현재 상태 (TODO)
            현재 title / body 모두 `"테스팅"` 고정값입니다.
            추후 `AlarmType`에 따라 아래와 같이 변경 예정입니다.

            | AlarmType | title | body |
            |-----------|-------|------|
            | PREPARE | (미정) | '출발 N분 전입니다. 준비하세요.' |
            | DEPARTURE | (미정) | '지금 출발하세요!' |
            | FIX | — | Notification 아닌 별도 처리로 분기 예정 |

            #### RabbitMQ 컨슈머 설정
            - prefetchCount: 30
            - batchSize: 15
            - concurrentConsumers: 3
            - acknowledgeMode: AUTO
            - receiveTimeout: 2,000ms
            - recoveryInterval: 30,000ms
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "FCM Notification 메시지 구조 참고",
        content = @Content(
            schema = @Schema(implementation = FcmAlarmNotificationSchema.class),
            examples = @ExampleObject(
                name = "DEPARTURE 알람 예시",
                value = """
                    {
                      "token": "fcm-device-token-abc",
                      "notification": {
                        "title": "테스팅",
                        "body": "테스팅"
                      }
                    }
                    """
            )
        )
    )
    @GetMapping("/alarm-notification")
    public FcmAlarmNotificationSchema alarmNotificationDoc() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, NOT_IMPL);
    }

    @Operation(
        summary = "FIX (경로 소멸) 알람 — Data-only Silent Push 메시지",
        description = """
            **발송 위치**: `NullRouteTicketWriter.write()` — `routeUpdateJob` Step 2

            **트리거**: 경로 갱신 배치 후 `routeData = null`인 RouteTicket 감지

            ---

            #### 메시지 특성
            - **Notification 포함**: ❌ NO — 시스템 트레이에 표시되지 않음 (silent)
            - **Data 페이로드**: ✅ `{ "alarmId": "5" }`
            - **APNs `content-available`**: ✅ 1 — iOS 백그라운드 실행 허용

            #### 클라이언트 처리
            앱이 백그라운드에서 `alarmId`를 수신하여 경로 재선택 화면을 표시합니다.
            (Android는 별도 백그라운드 처리 설정 필요 — 현재 APNs만 명시됨)

            #### 전송 전 사전 처리
            `RouteTicket.needCheck = true` 일괄 업데이트 후 FCM 발송
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "FCM Silent Push 메시지 구조 참고",
        content = @Content(
            schema = @Schema(implementation = FcmSilentPushSchema.class),
            examples = @ExampleObject(
                name = "경로 소멸 silent push 예시",
                value = """
                    {
                      "token": "fcm-device-token-abc",
                      "data": {
                        "alarmId": "5"
                      },
                      "apns": {
                        "aps": {
                          "content-available": true
                        }
                      }
                    }
                    """
            )
        )
    )
    @GetMapping("/fix-silent-push")
    public FcmSilentPushSchema fixSilentPushDoc() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, NOT_IMPL);
    }
}
