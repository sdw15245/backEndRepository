package com.sweep.project.alarm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmScheduler {

    private final JobLauncher jobLauncher;
    private final Job alarmPublishJob;

    /** 매 정각 삭제되지 않은 RouteTicket 을 FCM 토큰과 조합해 RabbitMQ 에 적재한다. */
    @Scheduled(cron = "0 0 * * * *")
    public void readyOnMessage() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", System.currentTimeMillis())
                    .addString("name", "alarmPublishJob")
                    .toJobParameters();
            jobLauncher.run(alarmPublishJob, params);
        } catch (Exception e) {
            log.error("알람 메시지 적재 batch 실행 실패: {}", e.getMessage());
        }
    }
}
