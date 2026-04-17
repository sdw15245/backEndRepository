package com.sweep.project.route.bus;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * getBusArrival XML 응답에서 추출한 핵심 도착 정보.
 * 정류소 정보, 버스 노선 ID/번호, 다음 버스 2대의 도착 예정 시각만 담는다.
 */
@Data
@AllArgsConstructor
public class BusArrivalInfo {

    /** 정류소 ID */
    private String stId;
    /** 정류소 이름 */
    private String stNm;
    /** 버스 노선 ID */
    private String busRouteId;
    /** 버스 노선 번호 (예: 3321) */
    private String rtNm;

    /** 첫 번째 버스 도착 예정 메시지 (예: "2분47초후[1번째 전]") */
    private String arrmsg1;
    /** 첫 번째 버스 도착 예정 시간 (초) */
    private int traTime1;

    /** 두 번째 버스 도착 예정 메시지 (예: "25분41초후[16번째 전]") */
    private String arrmsg2;
    /** 두 번째 버스 도착 예정 시간 (초) */
    private int traTime2;
}
