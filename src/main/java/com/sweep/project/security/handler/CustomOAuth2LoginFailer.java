package com.sweep.project.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweep.project.util.ApiResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
public class CustomOAuth2LoginFailer implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    public CustomOAuth2LoginFailer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponseUtil.FailApiResponse(exception.getMessage()));
        log.info("oauth2 로그인 실패: {}", exception.getMessage());
    }
}
