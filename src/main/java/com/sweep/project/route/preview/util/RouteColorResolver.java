package com.sweep.project.route.preview.util;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 교통수단별 라인 색상/스타일 규칙을 담당한다.
 */
@Component
public class RouteColorResolver {

    private static final String DEFAULT_WALK_COLOR = "#94a3b8";
    private static final String DEFAULT_BUS_COLOR = "#3b82f6";
    private static final String DEFAULT_SUBWAY_COLOR = "#f97316";

    /** 지하철 노선명 기준 색상 테이블 */
    private static final Map<String, String> SUBWAY_COLOR_MAP = Map.ofEntries(
            Map.entry("1호선", "#0052A4"),
            Map.entry("2호선", "#00A84D"),
            Map.entry("3호선", "#EF7C1C"),
            Map.entry("4호선", "#00A4E3"),
            Map.entry("5호선", "#996CAC"),
            Map.entry("6호선", "#CD7C2F"),
            Map.entry("7호선", "#747F00"),
            Map.entry("8호선", "#E6186C"),
            Map.entry("9호선", "#BDB092"),
            Map.entry("신분당선", "#D31145"),
            Map.entry("수인분당선", "#F5A200"),
            Map.entry("경의중앙선", "#77C4A3"),
            Map.entry("공항철도", "#0065B3")
    );

    /** ODsay busType 기준 색상 테이블 */
    private static final Map<Integer, String> BUS_TYPE_COLOR_MAP = Map.ofEntries(
            Map.entry(0, "#3b82f6"),
            Map.entry(1, "#ef4444"),
            Map.entry(2, "#3b82f6"),
            Map.entry(3, "#3b82f6"),
            Map.entry(4, "#ef4444"),
            Map.entry(5, "#ef4444"),
            Map.entry(6, "#10b981"),
            Map.entry(7, "#eab308")
    );

    /**
     * trafficType + laneName + busType에 따라 라인 색상을 반환한다.
     */
    public String resolveColor(int trafficType, String laneName, int busType) {
        return switch (trafficType) {
            case 1 -> resolveSubwayColor(laneName);
            case 2 -> BUS_TYPE_COLOR_MAP.getOrDefault(busType, DEFAULT_BUS_COLOR);
            case 3 -> DEFAULT_WALK_COLOR;
            default -> DEFAULT_BUS_COLOR;
        };
    }

    /**
     * trafficType에 따른 라인 스타일을 반환한다.
     */
    public String resolveStrokeStyle(int trafficType) {
        if (trafficType == 3) {
            return "shortdash";
        }
        return "solid";
    }

    private String resolveSubwayColor(String laneName) {
        if (laneName == null || laneName.isBlank()) {
            return DEFAULT_SUBWAY_COLOR;
        }

        // 노선명이 "수도권 2호선" 같이 붙어와도 포함 매칭으로 처리한다.
        for (Map.Entry<String, String> entry : SUBWAY_COLOR_MAP.entrySet()) {
            if (laneName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_SUBWAY_COLOR;
    }
}

