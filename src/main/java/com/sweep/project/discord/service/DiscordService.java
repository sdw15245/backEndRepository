package com.sweep.project.discord.service;

import com.sweep.project.discord.DiscordChannel;
import com.sweep.project.discord.DiscordEvent;

public interface DiscordService {

    void send(DiscordEvent discordEvent);
    DiscordChannel checkType();
}
