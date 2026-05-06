package com.sweep.project.route.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.redis.RouteRedisService;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.TrafficRouteStragy;
import com.sweep.project.route.domain.PathSearchType;
import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Slf4j
public class ItemWriter implements org.springframework.batch.item.ItemWriter<RouteBatchDto> {

    private final TrafficRouteStragy trafficRouteStragy;
    private final ObjectMapper objectMapper;
    private final RouteTicketRepo routeTicketRepo;
    private final LocalDateTime updateAt;

    @Override
    public void write(Chunk<? extends RouteBatchDto> chunk) throws Exception {

        Map<String, List<Long>> data = chunk.getItems().stream()
                .collect(Collectors.groupingBy(
                        this::key,
                        LinkedHashMap::new,
                        Collectors.mapping(RouteBatchDto::getId, Collectors.toList())
                ));
        Map<Long,String> id_JsonMap=new HashMap<>();


        data.forEach((key, ids) -> {
            RouteBatchDto parsed = parseKey(key);

            List<? extends TrafficResponse> trafficResponses =
                    trafficRouteStragy.getRoutes(
                            parsed.getType(),
                            parsed.getStartY(),
                            parsed.getStartX(),
                            parsed.getEndY(),
                            parsed.getEndX()
                    );


            IntStream.range(0,ids.size()).forEach(x->{
                try {
                    if(trafficResponses.get(x)==null){
                        id_JsonMap.put(ids.get(x),null);
                    }
                    else {
                        id_JsonMap.put(ids.get(x),
                                objectMapper.writeValueAsString(trafficResponses.get(x)));
                    }
                }
                catch (JsonProcessingException e){
                    log.info("파싱 에러:{}",e.getMessage());
                    throw new RuntimeException(e.getMessage());
                }
            });

        });

        routeTicketRepo.updateRouteBatch(id_JsonMap, updateAt);
    }

    private String key(RouteBatchDto dto) {
        return String.format("lock:route:%s:%.4f:%.4f:%.4f:%.4f",
                RouteRedisService.toTypeName(dto.getType()),
                dto.getStartX(),
                dto.getStartY(),
                dto.getEndX(),
                dto.getEndY());
    }

    private RouteBatchDto parseKey(String key) {
        // "lock:route:bus:37.1234:127.1234:37.5678:127.5678"
        String[] parts = key.split(":");
        PathSearchType type = switch (parts[2]) {
            case "bus"    -> PathSearchType.PATH_TYPE_BUS;
            case "subway" -> PathSearchType.PATH_TYPE_SUBWAY;
            case "mixed"  -> PathSearchType.PATH_TYPE_ANYONE;
            default -> throw new IllegalArgumentException("Unknown type name: " + parts[2]);
        };
        double startX = Double.parseDouble(parts[3]);
        double startY = Double.parseDouble(parts[4]);
        double endX   = Double.parseDouble(parts[5]);
        double endY   = Double.parseDouble(parts[6]);

        return new RouteBatchDto(null, type, startX, startY, endX, endY);
    }
}
