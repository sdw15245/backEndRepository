package com.sweep.project.route.batch;

import com.sweep.project.route.domain.PathSearchType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RouteBatchDto {
    private final Long id;
    private final PathSearchType type;
    private final double startX;
    private final double startY;
    private final double endX;
    private final double endY;
}
