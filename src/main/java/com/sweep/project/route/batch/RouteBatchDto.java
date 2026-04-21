package com.sweep.project.route.batch;

import com.sweep.project.route.domain.PathSearchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "경로 갱신 배치 처리 단위 DTO — RouteTicket에 연결된 Route 정보")
public class RouteBatchDto {

    @Schema(description = "Route ID", example = "10")
    private final Long id;

    @Schema(description = "경로 탐색 유형 (PATH_TYPE_ANYONE / PATH_TYPE_SUBWAY / PATH_TYPE_BUS)")
    private final PathSearchType type;

    @Schema(description = "출발지 경도 (소수점 4자리)", example = "126.9769")
    private final double startX;

    @Schema(description = "출발지 위도 (소수점 4자리)", example = "37.5796")
    private final double startY;

    @Schema(description = "목적지 경도 (소수점 4자리)", example = "127.0276")
    private final double endX;

    @Schema(description = "목적지 위도 (소수점 4자리)", example = "37.4979")
    private final double endY;
}
