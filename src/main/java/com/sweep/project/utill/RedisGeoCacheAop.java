package com.sweep.project.utill;

import com.sweep.project.route.ApiResponse;
import com.sweep.project.route.OdsayRouteResponse;
import com.sweep.project.route.PathSearchType;
import com.sweep.project.route.TrafficResponse;
import com.sweep.project.route.redis.RouteRedisService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Aspect
@RequiredArgsConstructor
public class RedisGeoCacheAop {


    private final RouteRedisService routeRedisService;

    //나중에 입력값으로 쿼리 파라미터에 서칭타입, 시작 위치경도, 종점 위치경도 를 받는 다는 가정하에
    //같은 위경도에 대해서 작동하는 캐싱 기반 프로세스.
    //위경도를 너무 세세하게 나누는것보다 소수점을 4자리에서끊어서 조금 비슷한 영역은 같은 취급하게 변경함.
    //구분하는 기준값==서칭타입, 시작 위치경도, 종점 위치경도
    @Around("@annotation(geoLocationCache)")
    public Object geoLocationCache(ProceedingJoinPoint joinPoint,GeoLocationCache geoLocationCache)
            throws Throwable{
        Object [] objects=joinPoint.getArgs();

        List<Double> pointList=Arrays.stream(objects).filter(x->{
            return x instanceof Double;
        }).map(x->{
            return (Double) x;
        }).toList();

        Optional<List<TrafficResponse>> apiResponseOptional=routeRedisService.find( (PathSearchType) objects[0]
                ,pointList.get(0),pointList.get(1),pointList.get(2),pointList.get(3));

       if(apiResponseOptional.isPresent()){
           return apiResponseOptional.get();
       }
        Object result = joinPoint.proceed();
       routeRedisService.saveIfAbsent( (PathSearchType) objects[0]
               ,pointList.get(0),pointList.get(1),pointList.get(2),pointList.get(3)
               ,(List<TrafficResponse>) result);

       return result;
    }

}
