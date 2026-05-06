package com.sweep.project.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmRequestDto {

    @Schema(description = "fcm 토큰값입니다")
    private String token;
}
