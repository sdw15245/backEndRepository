package com.sweep.project.security.handler;

import com.sweep.project.redis.RedisUserInfoService;
import com.sweep.project.util.jwt.JwtUtility;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
@Slf4j
public class CustomLogOutHandler implements LogoutSuccessHandler {

    private JwtUtility jwtUtility;
    private RedisUserInfoService redisUserInfoService;

    public CustomLogOutHandler(JwtUtility jwtUtility, RedisUserInfoService redisUserInfoService) {
        this.jwtUtility = jwtUtility;
        this.redisUserInfoService = redisUserInfoService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String authorization=request.getHeader(AUTHORIZATION);
        String token=jwtUtility.getTokenFromHeader(authorization);
        if(token==null){
            sendErrorResponse(response, HttpStatus.BAD_REQUEST,"토큰이 없습니다");
            return;
        }
        Claims claims=jwtUtility.getClaims(token);
        Long memberId=claims.get("id",Long.class);
        redisUserInfoService.logOutUserInfo(memberId);
        SecurityContextHolder.clearContext();
        log.info("로그아웃이 성공적으로 처리되었습니다");
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus httpStatus, String message) throws
            IOException {
        response.setStatus(httpStatus.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"message\":\"%s\"}", message, httpStatus.name()));
    }
}
