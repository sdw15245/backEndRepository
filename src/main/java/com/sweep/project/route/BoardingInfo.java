package com.sweep.project.route;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sweep.project.route.bus.BusBoardingInfo;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.subway.SubwayBoardingInfo;

/**
 * 탑승 정보 공통 마커 인터페이스.
 * SubwayBoardingInfo, BusBoardingInfo, MixedBoardingInfo 가 구현한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubwayBoardingInfo.class, name = "subway"),
        @JsonSubTypes.Type(value = BusBoardingInfo.class,    name = "bus"),
        @JsonSubTypes.Type(value = MixedBoardingInfo.class,  name = "mixed")
})
public interface BoardingInfo {
}
