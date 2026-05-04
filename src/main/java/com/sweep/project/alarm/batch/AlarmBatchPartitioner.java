package com.sweep.project.alarm.batch;

import com.sweep.project.alarm.repository.AlarmTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 활성 Alarm 의 alarmId 범위를 gridSize 개 파티션으로 분할한다.
 * 각 파티션은 minId / maxId 를 ExecutionContext 에 담아 슬레이브 스텝에 전달한다.
 */
@Slf4j
@RequiredArgsConstructor
public class AlarmBatchPartitioner implements Partitioner {

    private final AlarmTicketRepo alarmTicketRepo;
    private final LocalDateTime now;
    private final LocalDateTime next;


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        List<Long> ids = alarmTicketRepo.getActiveAlarmMinMaxId(now,next);
        Map<String, ExecutionContext> result = new LinkedHashMap<>();

        Long minId = ids.get(0);
        Long maxId = ids.get(1);

        if (minId == null || maxId == null) {
            log.info("[AlarmPartitioner] 처리할 활성 Alarm 없음 — 빈 파티션 반환");
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", 0L);
            ctx.putLong("maxId", -1L);
            result.put("alarmBatch0", ctx);
            return result;
        }

        long totalRange       = maxId - minId + 1;
        long rangePerPartition = Math.max(1, (long) Math.ceil((double) totalRange / gridSize));

        for (int i = 0; i < gridSize; i++) {
            long min = minId + ((long) i * rangePerPartition);
            long max = Math.min(min + rangePerPartition - 1, maxId);

            if (min > maxId) break;

            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", min);
            ctx.putLong("maxId", max);
            result.put("alarmBatch" + i, ctx);

            log.info("[AlarmPartitioner] 파티션 {} — minId={}, maxId={}", i, min, max);
        }
        return result;
    }
}
