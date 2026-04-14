package com.sweep.project.discord.service;

import com.sweep.project.discord.DiscordChannel;
import com.sweep.project.discord.DiscordEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
@Component
public class DiscordMqService extends AbstractDiscordService {
    @Value("${discord.mq-url}")
    private String mqAlertUrl;
    public DiscordMqService(RestTemplate restTemplate) {
        super(restTemplate);
    }
    public void sendAlert(DiscordEvent discordEvent){
        super.send(discordEvent);
    }
    @Override
    protected String getUrl() {
        return mqAlertUrl;
    }
    @Override
    protected DiscordChannel getChannel() {
        return DiscordChannel.MQ;
    }
    @Override
    public DiscordChannel checkType() {
        return DiscordChannel.MQ;
    }
}
