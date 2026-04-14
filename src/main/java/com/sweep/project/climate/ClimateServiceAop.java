package com.sweep.project.climate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.sweep.project.climate.ClimateGeoConverter.latLonToGrid;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ClimateServiceAop {

    //날씨정보는 넓은 범위를 같은 area취급을 하므로 조회시에 자칫잘못하면 api조회ㅓ가 대량으로 발생할수있어서 분산락적용
    private final RedissonClient redissonClient;
    private final static String climateKey="climate-";

    @Around("@annotation(com.sweep.project.climate.ClimateAop)")
    public Object climateInfoSearchLock(ProceedingJoinPoint point) throws Throwable{

        Object [] args=point.getArgs();
        double lat=(Double) args[0];
        double lon=(Double) args[1];
        int [] nxNy=latLonToGrid(lat,lon);
        RLock rLock;

        rLock=redissonClient.getLock(climateKey+nxNy[0]+nxNy[1]);
        try{
            //대기 시간 15초, 점유 가능시간 15초
            boolean rockState=rLock.tryLock(15000L,7000L, TimeUnit.MILLISECONDS);
            if(!rockState){
                throw new RuntimeException("대시시간 초과 발생, 재시도 해주세요");
            }
            log.info("redssion 트랜잭션 작동 시작");
            return point.proceed();
        }
        catch (Exception e){
            log.info("알수없는 에러발생:{}",e.getMessage());
            throw  e;
            //소켓 통신 에러 통합 컨트롤링 방법 생각하기.
        }
        finally {
            log.info("redssion lock 반납");
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }
}
