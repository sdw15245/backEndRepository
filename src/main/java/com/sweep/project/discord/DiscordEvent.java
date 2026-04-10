package com.sweep.project.discord;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DiscordEvent {

    private static final int COLOR_SUCCESS = 0x00FF00;
    private static final int COLOR_FAILURE = 0xFF0000;

    private List<Embed> embeds;

    private DiscordEvent(String title, String message, LocalDateTime when, int color) {
        this.embeds = List.of(
                Embed.builder()
                        .title(title)
                        .description(message)
                        .color(color)
                        .timestamp(when.toString())
                        .build()
        );
    }

    public static DiscordEvent success(String title, String message, LocalDateTime when) {
        return new DiscordEvent(title, message, when, COLOR_SUCCESS);
    }

    public static DiscordEvent failure(String title, String message, LocalDateTime when) {
        return new DiscordEvent(title, message, when, COLOR_FAILURE);
    }

    @Getter
    @Builder
    public static class Embed {
        private String title;
        private String description;
        private int color;
        private String timestamp;
    }
}
