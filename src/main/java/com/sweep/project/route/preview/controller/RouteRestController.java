package com.sweep.project.route.preview.controller;

import com.sweep.project.route.preview.dto.RoutePreviewDto;
import com.sweep.project.route.preview.service.RoutePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "Route Preview", description = "카카오맵 Polyline 렌더링을 위한 노선 미리보기 API. "
        + "ODsay 경로탐색 결과의 mapObj를 전달하면 구간별 좌표·색상·스타일을 반환합니다.")
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
@Slf4j
public class RouteRestController {

    private final RoutePreviewService routePreviewService;

    @Operation(
            summary = "노선 폴리라인 미리보기 조회",
            description = """
                    ODsay 경로탐색 API(/searchPubTransPathT 등)가 반환한 mapObj 값을 Path Variable로 전달합니다.

                    서버는 ODsay loadLane을 호출해 구간별 폴리라인 좌표를 가져온 뒤,
                    카카오맵 Polyline 옵션에 바로 적용할 수 있는 형태로 변환해 반환합니다.

                    ### mapObj 인코딩 규칙
                    - 원본 형식: `1:22@2:15` (콜론·앳 기호 포함)
                    - Path Variable 전달 시 URL 인코딩 필수: `1%3A22%402%3A15`
                    - 서버 내부에서 디코딩 처리하므로 이중 인코딩 불필요

                    ### 카카오맵 연동 예시 (JavaScript)
                    ```js
                    const res = await fetch(`/api/route/preview/${encodeURIComponent(mapObj)}`);
                    const { bounds, segments } = await res.json();

                    // 지도 범위 설정
                    map.setBounds(new kakao.maps.LatLngBounds(
                      new kakao.maps.LatLng(bounds.sw.y, bounds.sw.x),
                      new kakao.maps.LatLng(bounds.ne.y, bounds.ne.x)
                    ));

                    // 구간별 폴리라인 그리기
                    segments.forEach(seg => {
                      new kakao.maps.Polyline({
                        map,
                        path: seg.points.map(p => new kakao.maps.LatLng(p.y, p.x)),
                        strokeColor: seg.color,
                        strokeStyle: seg.strokeStyle,
                        strokeWeight: seg.trafficType === 3 ? 3 : 5,
                      }).setMap(map);
                    });
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "노선 폴리라인 데이터 반환 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoutePreviewDto.class),
                            examples = @ExampleObject(
                                    name = "지하철+버스 혼합 경로 예시",
                                    value = """
                                            {
                                              "routeId": "550e8400-e29b-41d4-a716-446655440000",
                                              "bounds": {
                                                "sw": { "x": 126.9784, "y": 37.5660 },
                                                "ne": { "x": 127.0276, "y": 37.5900 }
                                              },
                                              "segments": [
                                                {
                                                  "trafficType": 1,
                                                  "trafficTypeLabel": "SUBWAY",
                                                  "laneName": "2호선",
                                                  "color": "#00A84D",
                                                  "strokeStyle": "solid",
                                                  "points": [
                                                    { "x": 126.9784, "y": 37.5660 },
                                                    { "x": 126.9850, "y": 37.5720 }
                                                  ]
                                                },
                                                {
                                                  "trafficType": 3,
                                                  "trafficTypeLabel": "WALK",
                                                  "laneName": "",
                                                  "color": "#94a3b8",
                                                  "strokeStyle": "shortdash",
                                                  "points": [
                                                    { "x": 126.9850, "y": 37.5720 },
                                                    { "x": 126.9870, "y": 37.5740 }
                                                  ]
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "mapObj에 해당하는 노선 데이터가 ODsay에 존재하지 않음",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "ODsay loadLane API 호출 실패 (upstream 오류)",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/preview/{mapObj}")
    public RoutePreviewDto getPreview(
            @Parameter(
                    name = "mapObj",
                    description = "ODsay 경로탐색 결과의 mapObj 값\n\n"
                            + "ODsay /searchPubTransPathT 응답의 `result.path[n].info.mapObj` 필드를 그대로 URL 전달\n\n"
                            + "원본: `1:22@2:15` → 인코딩: `1%3A22%402%3A15`",
                    required = true,
                    example = "1%3A22%402%3A15",
                    schema = @Schema(type = "string")
            )
            @PathVariable String mapObj) {
        String decodedMapObj = URLDecoder.decode(mapObj, StandardCharsets.UTF_8);
        log.info("decodingmapobj:{}",decodedMapObj);
        return routePreviewService.getRoutePreview(decodedMapObj);
    }
}

