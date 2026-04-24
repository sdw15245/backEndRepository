package com.sweep.project.route.preview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "카카오맵 Polyline 렌더링용 노선 미리보기 응답. "
        + "segments 배열을 순서대로 그리면 출발지→목적지 전체 경로가 완성됩니다.")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutePreviewDto {

    @Schema(
            description = "요청마다 서버에서 생성하는 UUID. 클라이언트가 폴리라인 객체를 식별·정리할 때 활용합니다.",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String routeId;

    @Schema(description = "전체 경로를 포함하는 최소 경계 박스. 카카오맵 setBounds() 에 그대로 사용합니다.")
    private BoundsDto bounds;

    @Schema(description = "교통수단 구간별 폴리라인 목록. 배열 순서가 이동 순서와 일치합니다.")
    private List<SegmentDto> segments;

    @Schema(description = "단일 교통수단 구간의 폴리라인 정보. "
            + "카카오맵 Polyline 옵션(strokeColor, strokeStyle, path)에 바로 매핑됩니다.")
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SegmentDto {

        @Schema(
                description = "ODsay trafficType 원본 코드. 1=지하철, 2=버스, 3=도보",
                allowableValues = {"1", "2", "3"},
                example = "1"
        )
        private int trafficType;

        @Schema(
                description = "trafficType의 문자열 레이블",
                allowableValues = {"SUBWAY", "BUS", "WALK"},
                example = "SUBWAY"
        )
        private String trafficTypeLabel;

        @Schema(
                description = "지하철 노선명(예: 2호선) 또는 버스 번호(예: 146). 도보 구간은 빈 문자열입니다.",
                example = "2호선"
        )
        private String laneName;

        @Schema(
                description = "카카오맵 strokeColor에 사용할 HEX 색상 코드. "
                        + "지하철은 공식 노선색, 버스는 유형별 색상, 도보는 회색(#94a3b8)입니다.",
                example = "#00A84D",
                pattern = "^#[0-9A-Fa-f]{6}$"
        )
        private String color;

        @Schema(
                description = "카카오맵 Polyline strokeStyle 값. 지하철·버스는 solid, 도보는 shortdash",
                allowableValues = {"solid", "shortdash"},
                example = "solid"
        )
        private String strokeStyle;

        @Schema(description = "이 구간의 폴리라인 좌표 목록. 배열 순서가 이동 방향입니다.")
        private List<PointDto> points;
    }

    @Schema(description = "지도 좌표 한 점. 카카오맵 LatLng(y, x) 순서로 전달해야 합니다.")
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PointDto {

        @Schema(
                description = "경도 (longitude). 카카오맵 LatLng의 두 번째 인자(lng)에 해당합니다.",
                example = "127.02760"
        )
        private double x;

        @Schema(
                description = "위도 (latitude). 카카오맵 LatLng의 첫 번째 인자(lat)에 해당합니다.",
                example = "37.49794"
        )
        private double y;
    }

    @Schema(description = "전체 경로의 남서(SW)·북동(NE) 꼭짓점 좌표. "
            + "kakao.maps.LatLngBounds(sw, ne) 로 변환해 map.setBounds()에 사용합니다.")
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BoundsDto {

        @Schema(description = "경계 박스의 남서쪽(좌하단) 꼭짓점 좌표")
        private PointDto sw;

        @Schema(description = "경계 박스의 북동쪽(우상단) 꼭짓점 좌표")
        private PointDto ne;
    }
}

