package com.sweep.project.route.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.fcm.service.FcmSendService;
import com.sweep.project.route.TrafficRouteStragy;
import com.sweep.project.route.domain.RouteTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RouteBatchConfig {

    private static final int CHUNK_SIZE = 100;
    private static final int GRID_SIZE  = 4;

    private final RouteTicketRepo routeTicketRepo;
    private final TrafficRouteStragy trafficRouteStragy;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FcmSendService fcmSendService;

    // ── Job ───────────────────────────────────────────────────────────────────

    @Bean
    public Job routeUpdateJob(Step masterStep, Step alarmNeedCheckMasterStep) {
        return new JobBuilder("routeUpdateJob", jobRepository)
                .listener(updateAtListener())
                .start(masterStep)
                .next(alarmNeedCheckMasterStep)
                .build();
    }

    @Bean
    public JobExecutionListener updateAtListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                jobExecution.getExecutionContext().put("updateAt", LocalDateTime.now());
            }
        };
    }

    // ── Master step (partitioner) ──────────────────────────────────────────

    @Bean
    public Step masterStep(Step slaveStep, Partitioner routeBatchPartitioner) {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("slaveStep", routeBatchPartitioner)
                .step(slaveStep)
                .gridSize(GRID_SIZE)
                .build();
    }

    // ── Slave step (chunk-oriented) ────────────────────────────────────────

    @Bean
    public Step slaveStep(ZeroOffSetReader zeroOffSetReader,
                          ItemProcessor itemProcessor,
                          ItemWriter itemWriter) {
        return new StepBuilder("slaveStep", jobRepository)
                .<RouteBatchDto, RouteBatchDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(zeroOffSetReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    // ── Beans ──────────────────────────────────────────────────────────────

    @Bean
    @JobScope
    public Partitioner routeBatchPartitioner() {
        return new RoutePartitioner(routeTicketRepo);
    }

    @Bean
    @StepScope
    public ZeroOffSetReader zeroOffSetReader() {
        return new ZeroOffSetReader(routeTicketRepo, CHUNK_SIZE);
    }

    @Bean
    @StepScope
    public ItemProcessor itemProcessor() {
        return new ItemProcessor();
    }

    @Bean
    @StepScope
    public ItemWriter itemWriter(
            @Value("#{jobExecutionContext['updateAt']}") LocalDateTime updateAt) {
        return new ItemWriter(trafficRouteStragy, objectMapper, routeTicketRepo, updateAt);
    }

    // ── AlarmNeedCheck master step (경로 갱신 후 알람 체크 필요 감지) ───────────

    @Bean
    public Step alarmNeedCheckMasterStep(Step alarmNeedCheckSlaveStep, Partitioner alarmNeedCheckPartitioner) {
        return new StepBuilder("alarmNeedCheckMasterStep", jobRepository)
                .partitioner("alarmNeedCheckSlaveStep", alarmNeedCheckPartitioner)
                .step(alarmNeedCheckSlaveStep)
                .gridSize(GRID_SIZE)
                .build();
    }

    // ── AlarmNeedCheck slave step (chunk-oriented) ────────────────────────────

    @Bean
    public Step alarmNeedCheckSlaveStep(AlarmNeedCheckReader alarmNeedCheckReader,
                                        AlarmNeedCheckWriter alarmNeedCheckWriter) {
        return new StepBuilder("alarmNeedCheckSlaveStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, transactionManager)
                .reader(alarmNeedCheckReader)
                .writer(alarmNeedCheckWriter)
                .build();
    }

    // ── AlarmNeedCheck beans ──────────────────────────────────────────────────

    @Bean
    @JobScope
    public Partitioner alarmNeedCheckPartitioner(
            @Value("#{jobExecutionContext['updateAt']}") LocalDateTime updateAt) {
        return new AlarmNeedCheckPartitioner(routeTicketRepo, updateAt);
    }

    @Bean
    @StepScope
    public AlarmNeedCheckReader alarmNeedCheckReader(
            @Value("#{jobExecutionContext['updateAt']}") LocalDateTime updateAt) {
        return new AlarmNeedCheckReader(routeTicketRepo, CHUNK_SIZE, updateAt);
    }

    @Bean
    @StepScope
    public AlarmNeedCheckWriter alarmNeedCheckWriter() {
        return new AlarmNeedCheckWriter(routeTicketRepo, fcmSendService);
    }
}
