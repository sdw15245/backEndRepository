package com.sweep.project.util;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApiResponseUtil<T> {

    private String msg;
    private T data;

    public ApiResponseUtil(String msg, T data) {
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResponseUtil<T> SuccessApiResponse(String msg, T data){
        return new ApiResponseUtil<T>(msg,data);
    }
    public static<T> ApiResponseUtil<T> FailApiResponse(String msg){
        return new ApiResponseUtil<>(msg,null);
    }
}
