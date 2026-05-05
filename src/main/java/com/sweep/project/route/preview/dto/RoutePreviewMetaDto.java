package com.sweep.project.route.preview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutePreviewMetaDto {

    private String mapObj;
    private List<CachedSegmentDto> segments;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CachedSegmentDto {
        private int trafficType;
        private String laneName;
        private Integer busType;
        private String startName;
        private String endName;
    }
}
