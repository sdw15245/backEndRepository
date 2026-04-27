package com.sweep.project.route;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sweep.project.route.bus.BusRoute;
import com.sweep.project.route.mixed.MixedRoute;
import com.sweep.project.route.subway.SubwayRoute;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubwayRoute.class, name = "subway"),
        @JsonSubTypes.Type(value = BusRoute.class,    name = "bus"),
        @JsonSubTypes.Type(value = MixedRoute.class,  name = "mixed")
})
@Schema(oneOf = {SubwayRoute.class, BusRoute.class, MixedRoute.class},
        discriminatorProperty = "@type")
public interface TrafficResponse {
    Long getRouteId();
    void setRouteId(Long routeId);
}
