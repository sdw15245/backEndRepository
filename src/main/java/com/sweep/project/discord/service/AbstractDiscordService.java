package com.sweep.project.discord.service;

import com.sweep.project.discord.DiscordChannel;
import com.sweep.project.discord.DiscordEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDiscordService implements DiscordService {

    protected abstract DiscordChannel getChannel();
    protected abstract String getUrl();
    private final RestTemplate restTemplate;


    @Retryable(maxAttempts =4,backoff =@Backoff(delay = 1000L,maxDelay = 16000L,multiplier = 4.0),
            recover = "sendMessageFailFinally")
    @Override
    public void send(DiscordEvent discordEvent) {
        restTemplate.postForEntity(getUrl(),discordEvent,Void.class);
    }


    @Recover
    public void sendMessageFailFinally(Exception e){
        log.error("discord channel 최종 실패 {}-{}",getChannel().name(),e.getMessage());
    }
}
