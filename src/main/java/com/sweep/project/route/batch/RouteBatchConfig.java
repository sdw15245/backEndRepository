package com.sweep.project.route.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    // ── Job ───────────────────────────────────────────────────────────────────

    @Bean
    public Job routeUpdateJob(Step masterStep, Step nullRouteMasterStep) {
        return new JobBuilder("routeUpdateJob", jobRepository)
                .listener(updateAtListener())
                .start(masterStep)
                .next(nullRouteMasterStep)
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

    // ── NullRouteCheck master step (partitioner) ──────────────────────────────

    @Bean
    public Step nullRouteMasterStep(Step nullRouteSlaveStep, Partitioner nullRoutePartitioner) {
        return new StepBuilder("nullRouteMasterStep", jobRepository)
                .partitioner("nullRouteSlaveStep", nullRoutePartitioner)
                .step(nullRouteSlaveStep)
                .gridSize(GRID_SIZE)
                .build();
    }

    // ── NullRouteCheck slave step (chunk-oriented) ────────────────────────────

    @Bean
    public Step nullRouteSlaveStep(NullRouteTicketReader nullRouteTicketReader,
                                   NullRouteTicketWriter nullRouteTicketWriter) {
        return new StepBuilder("nullRouteSlaveStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, transactionManager)
                .reader(nullRouteTicketReader)
                .writer(nullRouteTicketWriter)
                .build();
    }

    // ── NullRouteCheck beans ──────────────────────────────────────────────────

    @Bean
    @JobScope
    public Partitioner nullRoutePartitioner(
            @Value("#{jobExecutionContext['updateAt']}") LocalDateTime updateAt) {
        return new NullRoutePartitioner(routeTicketRepo, updateAt);
    }

    @Bean
    @StepScope
    public NullRouteTicketReader nullRouteTicketReader(
            @Value("#{jobExecutionContext['updateAt']}") LocalDateTime updateAt) {
        return new NullRouteTicketReader(routeTicketRepo, CHUNK_SIZE, updateAt);
    }

    @Bean
    @StepScope
    public NullRouteTicketWriter nullRouteTicketWriter() {
        return new NullRouteTicketWriter(routeTicketRepo);
    }
}
