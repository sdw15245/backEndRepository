package com.sweep.project.route.bus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 서울 버스 도착 정보 API (getArrInfoByRoute) 응답 DTO.
 *
 * <p>endpoint: http://ws.bus.go.kr/api/rest/arrive/getArrInfoByRoute</p>
 * <p>request: serviceKey, stId(정류소ID), busRouteId(노선ID), ord(정류소순번)</p>
 *
 * <p>하나의 노선+정류소 조합에 대해 다음 버스 2대의 도착 예정 정보를 반환한다.
 * 두 버스 정보는 별도 객체가 아닌 번호 접미사(1/2) 필드로 구분된다.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusArrivalResponse {

    @JsonProperty("ServiceResult")
    private ServiceResult serviceResult;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceResult {
        private MsgHeader msgHeader;
        private MsgBody msgBody;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MsgHeader {
        /** 0: 정상 */
        private String headerCd;
        private String headerMsg;
        private String itemCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MsgBody {
        /** 단일 노선+정류소 결과 (배열 형태로 반환될 수 있어 List로 선언) */
        private List<Item> itemList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        // ── 공통 정보 ──────────────────────────────────────────────────
        /** 조회 정류소 이름 */
        private String stNm;
        /** 다음 정류소 이름 */
        private String nxtStn;
        /** 버스 번호 약어 (예: 6714) */
        private String busRouteAbrv;
        /** 배차 간격 (분) */
        private String term;
        /** 노선 유형 (11: 간선, 12: 지선, 13: 순환, 14: 광역 등) */
        private String routeType;

        // ── 첫 번째 버스 ───────────────────────────────────────────────
        /** 도착 예정 메시지 (예: "2분 30초 후 [2번째 전]") */
        private String arrmsg1;
        /** 도착 예정 시간 (초) */
        private String traTime1;
        /** 차량 번호판 */
        private String plainNo1;
        /** 버스 유형 (0: 일반, 1: 저상) */
        private String busType1;
        /** 혼잡도 (0: 정보없음, 3: 여유, 4: 보통, 5: 혼잡, 6: 매우혼잡) */
        private String congestdeg1;
        /** 막차 여부 (0: 일반, 1: 막차) */
        private String isLast1;
        /** 현재 버스가 위치한 정류소명 */
        private String prvSttn1;
        /** 탑승 정류소까지 남은 정류소 수 */
        @JsonProperty("rerdie_Num1")
        private String rerdieNum1;
        /** GPS X 좌표 (경도) */
        private String gpsX1;
        /** GPS Y 좌표 (위도) */
        private String gpsY1;
        /** 현재 구간 순번 */
        private String sectOrd1;

        // ── 두 번째 버스 ───────────────────────────────────────────────
        /** 도착 예정 메시지 (예: "8분 후 [5번째 전]") */
        private String arrmsg2;
        /** 도착 예정 시간 (초) */
        private String traTime2;
        /** 차량 번호판 */
        private String plainNo2;
        /** 버스 유형 */
        private String busType2;
        /** 혼잡도 */
        private String congestdeg2;
        /** 막차 여부 */
        private String isLast2;
        /** 현재 버스가 위치한 정류소명 */
        private String prvSttn2;
        /** 탑승 정류소까지 남은 정류소 수 */
        @JsonProperty("rerdie_Num2")
        private String rerdieNum2;
        /** GPS X 좌표 (경도) */
        private String gpsX2;
        /** GPS Y 좌표 (위도) */
        private String gpsY2;
        /** 현재 구간 순번 */
        private String sectOrd2;
    }
}
