package com.sweep.project.route.dto;

import com.sweep.project.route.BoardingInfo;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.domain.PathSearchType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteTicketResponse {

    private Long ticketId;
    private Long routeId;

    private PathSearchType type;

    /** Redis 또는 DB 에서 조회한 경로 데이터 */
    private TrafficResponse routeData;

    /**
     * 탑승 정보 — subway 타입일 때만 Redis 에서 조회하여 채워진다.
     * bus / mixed 이거나 Redis 에 데이터가 없으면 null.
     */
    private List<BoardingInfo> boardingInfos;
}
