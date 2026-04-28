package com.sweep.project.route;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sweep.project.route.bus.BusBoardingInfo;
import com.sweep.project.route.mixed.MixedBoardingInfo;
import com.sweep.project.route.subway.SubwayBoardingInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubwayBoardingInfo.class, name = "subway"),
        @JsonSubTypes.Type(value = BusBoardingInfo.class,    name = "bus"),
        @JsonSubTypes.Type(value = MixedBoardingInfo.class,  name = "mixed")
})
@Schema(oneOf = {SubwayBoardingInfo.class, BusBoardingInfo.class, MixedBoardingInfo.class},
        discriminatorProperty = "@type")
public interface BoardingInfo {
}
