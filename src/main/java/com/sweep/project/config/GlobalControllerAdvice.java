package com.sweep.project.config;

import com.sweep.project.util.ApiResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> globalException(Exception e){
        return new ResponseEntity<>(ApiResponseUtil.FailApiResponse(e.getMessage())
                ,null, HttpStatus.BAD_REQUEST);
    }
}
