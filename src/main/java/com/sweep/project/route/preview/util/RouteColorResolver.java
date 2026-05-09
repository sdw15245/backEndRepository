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

    private static final Map<Integer, String> SUBWAY_TYPE_COLOR_MAP = Map.ofEntries(
            Map.entry(1, "#263C96"),
            Map.entry(2, "#3CB44A"),
            Map.entry(3, "#F06E00"),
            Map.entry(4, "#2C9EDE"),
            Map.entry(5, "#8936E0"),
            Map.entry(6, "#B5500B"),
            Map.entry(7, "#697215"),
            Map.entry(8, "#E51E6E"),
            Map.entry(9, "#D1A62C"),
            Map.entry(21, "#6F99D0"),
            Map.entry(22, "#F4AB3E"),
            Map.entry(31, "#3AB449"),
            Map.entry(41, "#E51E6E"),
            Map.entry(42, "#3AB449"),
            Map.entry(43, "#FCA92F"),
            Map.entry(48, "#2673F2"),
            Map.entry(51, "#3AB449"),
            Map.entry(71, "#F77636"),
            Map.entry(72, "#3AB449"),
            Map.entry(73, "#C9A754"),
            Map.entry(74, "#1A80E5"),
            Map.entry(78, "#799BC9"),
            Map.entry(79, "#80499C"),
            Map.entry(91, "#905A89"),
            Map.entry(101, "#73B6E4"),
            Map.entry(102, "#FF9D5A"),
            Map.entry(104, "#76BC9E"),
            Map.entry(107, "#77C371"),
            Map.entry(108, "#08AF7B"),
            Map.entry(109, "#A71E31"),
            Map.entry(110, "#FF9D27"),
            Map.entry(112, "#2673F2"),
            Map.entry(113, "#C6C100"),
            Map.entry(114, "#8BC53F"),
            Map.entry(115, "#96710A"),
            Map.entry(116, "#EBA900"),
            Map.entry(117, "#4E67A5")
    );

    /** 지하철 노선명 기준 색상 테이블 */
    private static final Map<String, String> SUBWAY_COLOR_MAP = Map.ofEntries(
            Map.entry("1호선", "#263C96"),
            Map.entry("2호선", "#3CB44A"),
            Map.entry("3호선", "#F06E00"),
            Map.entry("4호선", "#2C9EDE"),
            Map.entry("5호선", "#8936E0"),
            Map.entry("6호선", "#B5500B"),
            Map.entry("7호선", "#697215"),
            Map.entry("8호선", "#E51E6E"),
            Map.entry("9호선", "#D1A62C"),
            Map.entry("GTX-A", "#905A89"),
            Map.entry("공항철도", "#73B6E4"),
            Map.entry("자기부상철도", "#FF9D5A"),
            Map.entry("경의중앙선", "#76BC9E"),
            Map.entry("에버라인", "#77C371"),
            Map.entry("경춘선", "#08AF7B"),
            Map.entry("신분당선", "#A71E31"),
            Map.entry("의정부경전철", "#FF9D27"),
            Map.entry("경강선", "#2673F2"),
            Map.entry("우이신설선", "#C6C100"),
            Map.entry("서해선", "#8BC53F"),
            Map.entry("김포골드라인", "#96710A"),
            Map.entry("수인분당선", "#EBA900"),
            Map.entry("신림선", "#4E67A5"),
            Map.entry("대경선", "#2673F2"),
            Map.entry("동해선", "#799BC9"),
            Map.entry("부산-김해경전철", "#80499C")
    );

    /** ODsay busType 기준 색상 테이블 */
    private static final Map<Integer, String> BUS_TYPE_COLOR_MAP = Map.ofEntries(
            Map.entry(0, "#3B82F6"),
            Map.entry(1, "#3B82F6"),
            Map.entry(2, "#C62828"),
            Map.entry(3, "#53B332"),
            Map.entry(4, "#D32F2F"),
            Map.entry(5, "#0068B7"),
            Map.entry(6, "#7B1FA2"),
            Map.entry(10, "#607D8B"),
            Map.entry(11, "#0068B7"),
            Map.entry(12, "#53B332"),
            Map.entry(13, "#F9A825"),
            Map.entry(14, "#E60012"),
            Map.entry(15, "#FF8C00"),
            Map.entry(16, "#8E44AD"),
            Map.entry(20, "#7CB342"),
            Map.entry(22, "#B71C1C"),
            Map.entry(26, "#6A1B9A"),
            Map.entry(30, "#00A6B4")
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

    public String resolveColorByLaneType(int trafficType, int laneType) {
        return switch (trafficType) {
            case 1 -> SUBWAY_TYPE_COLOR_MAP.getOrDefault(laneType, DEFAULT_SUBWAY_COLOR);
            case 2 -> BUS_TYPE_COLOR_MAP.getOrDefault(laneType, DEFAULT_BUS_COLOR);
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

