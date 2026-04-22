package com.sweep.project.route.mixed;

import com.sweep.project.route.BoardingInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 복합(버스+지하철) 경로 전체 탑승 정보.
 * <p>
 * segmentBoardingInfos 의 첫 번째 항목(transferPoint=false)이 최초 탑승 정보이고,
 * 나머지 항목(transferPoint=true)이 환승 지점별 탑승 정보이다.
 */
@Data
@AllArgsConstructor
@Schema(description = "복합(버스+지하철) 경로 탑승 정보")
public class MixedBoardingInfo implements BoardingInfo {

    @Schema(description = "출발지에서 출발해야 하는 권장 시각", example = "08:20:00")
    private LocalTime recommendedDepartureTime;

    @Schema(description = "교통 수단 구간별 탑승 정보 목록. index 0: 최초 탑승, index 1+: 환승 지점")
    private List<SegmentBoardingInfo> segmentBoardingInfos;
}
