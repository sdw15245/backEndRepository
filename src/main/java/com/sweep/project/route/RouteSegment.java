package com.sweep.project.route;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sweep.project.route.bus.BusRoute;
import com.sweep.project.route.domain.WalkSegment;
import com.sweep.project.route.subway.SubwayRoute;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 경로 구간 공통 인터페이스
 * trafficType: 1 = 지하철, 2 = 버스, 3 = 도보
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WalkSegment.class,             name = "walk"),
        @JsonSubTypes.Type(value = SubwayRoute.SubwaySegment.class, name = "subway"),
        @JsonSubTypes.Type(value = BusRoute.BusSegment.class,      name = "bus")
})
@Schema(oneOf = {WalkSegment.class, SubwayRoute.SubwaySegment.class, BusRoute.BusSegment.class},
        discriminatorProperty = "@type")
public interface RouteSegment {
    int getTrafficType();
    int getSectionTime();
    int getDistance();
}
