package com.sweep.project.route.bus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * getBusArrival XML 응답에서 추출한 핵심 도착 정보.
 * 정류소 정보, 버스 노선 ID/번호, 다음 버스 2대의 도착 예정 시각만 담는다.
 */
@Data
@AllArgsConstructor
public class BusArrivalInfo {

    @Schema(description = "버스 정류소 id값")
    /** 정류소 ID */
    private String stId;
    @Schema(description = "버스 정류소 이름")
    /** 정류소 이름 */
    private String stNm;
    @Schema(description = "버스 정류소 노선 id")
    /** 버스 노선 ID */
    private String busRouteId;
    @Schema(description = "버스 정류소 노선 번호")
    /** 버스 노선 번호 (예: 3321) */
    private String rtNm;
    @Schema(description = "도촥 관련 메시지",example = "2분 47초후")
    /** 첫 번째 버스 도착 예정 메시지 (예: "2분47초후[1번째 전]") */
    private String arrmsg1;
    @Schema(description = "초단위로 도착까지 얼마나 남았는가")
    /** 첫 번째 버스 도착 예정 시간 (초) */
    private int traTime1;
    @Schema(description = "두 번째 버스 도착 예정 메시지", example = "25분 41초후 [16번째 전]")
    private String arrmsg2;
    @Schema(description = "두 번째 버스 도착 예정 시간 (초)", example = "1541")
    private int traTime2;
}
