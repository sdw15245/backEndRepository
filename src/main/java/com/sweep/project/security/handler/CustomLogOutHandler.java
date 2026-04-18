package com.sweep.project.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.redis.RedisUserInfoService;
import com.sweep.project.util.ApiResponseUtil;
import com.sweep.project.util.jwt.JwtUtility;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class CustomLogOutHandler implements LogoutSuccessHandler {

    private final JwtUtility jwtUtility;
    private final RedisUserInfoService redisUserInfoService;
    private final ObjectMapper objectMapper;

    public CustomLogOutHandler(JwtUtility jwtUtility, RedisUserInfoService redisUserInfoService, ObjectMapper objectMapper) {
        this.jwtUtility = jwtUtility;
        this.redisUserInfoService = redisUserInfoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String authorization = request.getHeader(AUTHORIZATION);
        String token = jwtUtility.getTokenFromHeader(authorization);
        if (token == null) {
            sendResponse(response, HttpStatus.BAD_REQUEST, ApiResponseUtil.FailApiResponse("토큰이 없습니다"));
            return;
        }
        Claims claims = jwtUtility.getClaims(token);
        Long memberId = claims.get("id", Long.class);
        redisUserInfoService.logOutUserInfo(memberId);
        SecurityContextHolder.clearContext();
        log.info("로그아웃이 성공적으로 처리되었습니다");
        sendResponse(response, HttpStatus.OK, ApiResponseUtil.SuccessApiResponse("로그아웃 되었습니다", null));
    }

    private void sendResponse(HttpServletResponse response, HttpStatus status, ApiResponseUtil<?> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
