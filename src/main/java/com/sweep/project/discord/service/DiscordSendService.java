package com.sweep.project.discord.service;

import com.sweep.project.discord.DiscordChannel;
import com.sweep.project.discord.DiscordEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DiscordSendService {
    private final Map<DiscordChannel, DiscordService> discordChannelDiscordServiceMap=new HashMap<>();
    @Autowired
    private TaskExecutor asyncSendThread;


    public DiscordSendService(List<DiscordService> discordServiceList) {
        for(DiscordService discordService:discordServiceList){
            discordChannelDiscordServiceMap.put(discordService.checkType(),discordService);
        }
    }
    public void sendAlert(DiscordChannel discordChannel, DiscordEvent discordEvent){
        DiscordService discordService=discordChannelDiscordServiceMap.get(discordChannel);
        if(discordService==null){
            log.info("적합한 디스코드 서비스가없습니다:{}",discordChannel);
        }
        else{
            CompletableFuture.runAsync(()->
                    {discordService.send(discordEvent);},asyncSendThread)
                    .exceptionally(e->{
                        log.error("discord channel 에러 발생:{}-{}",discordService.checkType().name()
                                ,e.getMessage());
                        return null;
                    });
        }
    }
}
