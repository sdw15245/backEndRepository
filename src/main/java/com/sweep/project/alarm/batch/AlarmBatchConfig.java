package com.sweep.project.alarm.batch;

import com.sweep.project.alarm.repository.AlarmTicketRepo;
import com.sweep.project.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알람 배치 Job 설정.
 *
 * <pre>
 * alarmPublishJob
 *   └─ alarmMasterStep  (Partitioner, gridSize=4)
 *        └─ alarmSlaveStep  (chunk=100)
 *             ├─ AlarmZeroOffsetReader  : deleted=false Alarm 스트리밍
 *             └─ AlarmBatchWriter       : memberId 그룹핑 → FCM 토큰 조합
 *                                        → 준비/출발 알람 계산 → TTL RabbitMQ 발행
 * </pre>
 *
 * <p>TTL 기준: JobExecutionContext["schedulerRunAt"] (Job 시작 시각)
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class AlarmBatchConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int GRID_SIZE  = 4;

    private final AlarmTicketRepo alarmTicketRepo;
    private final FcmTokenRepository fcmTokenRepository;
    private final StringRedisTemplate redisTemplate;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // ── Job ───────────────────────────────────────────────────────────────────

    @Bean
    public Job alarmPublishJob(Step alarmMasterStep) {
        return new JobBuilder("alarmPublishJob", jobRepository)
                .listener(alarmJobListener())
                .start(alarmMasterStep)
                .build();
    }

    /**
     * Job 시작 시각을 ExecutionContext 에 저장한다.
     * AlarmBatchWriter 에서 TTL 계산의 기준 시각으로 사용된다.
     */
    @Bean
    public JobExecutionListener alarmJobListener() {
        LocalDateTime now=LocalDateTime.now();
        LocalDateTime after=now.plusDays(1L);
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                jobExecution.getExecutionContext().put("schedulerRunAt",now);
                jobExecution.getExecutionContext().put("nextSchedulerRunAt",after);
            }
        };
    }

    // ── Master step (Partitioner) ─────────────────────────────────────────────

    @Bean
    public Step alarmMasterStep(Step alarmSlaveStep, Partitioner alarmBatchPartitioner) {
        return new StepBuilder("alarmMasterStep", jobRepository)
                .partitioner("alarmSlaveStep", alarmBatchPartitioner)
                .step(alarmSlaveStep)
                .gridSize(GRID_SIZE)
                .build();
    }

    // ── Slave step (chunk-oriented, processor 없음) ───────────────────────────

    @Bean
    public Step alarmSlaveStep(AlarmZeroOffsetReader alarmZeroOffsetReader,
                               AlarmBatchWriter alarmBatchWriter) {
        return new StepBuilder("alarmSlaveStep", jobRepository)
                .<AlarmBatchDto, AlarmBatchDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(alarmZeroOffsetReader)
                .writer(alarmBatchWriter)
                .build();
    }

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean
    @JobScope
    public Partitioner alarmBatchPartitioner(
            @Value("#{jobExecutionContext['schedulerRunAt']}") LocalDateTime schedulerRunAt,
            @Value("#{jobExecutionContext['nextSchedulerRunAt']}")LocalDateTime after) {
        return new AlarmBatchPartitioner(alarmTicketRepo,schedulerRunAt,after);
    }

    @Bean
    @StepScope
    public AlarmZeroOffsetReader alarmZeroOffsetReader() {
        return new AlarmZeroOffsetReader(alarmTicketRepo, CHUNK_SIZE);
    }

    @Bean
    @StepScope
    public AlarmBatchWriter alarmBatchWriter(
            @Value("#{jobExecutionContext['schedulerRunAt']}") LocalDateTime schedulerRunAt) {
        return new AlarmBatchWriter(redisTemplate, fcmTokenRepository, schedulerRunAt);
    }
}
