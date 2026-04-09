package com.sweep.project.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class RedisUserInfoService {

    private final RedisTemplate<String,String> redisTemplate;
    private final static String userInfoKey="member-info-key-";
    private final static String userRefreshTokenKey="member-refresh-key-";
    private final StringRedisTemplate stringRedisTemplate;

    public void setLoginUserInfo(Long id,String member,String refreshToken){
        stringRedisTemplate.execute(new DefaultRedisScript<>(RedisLuaScript.setLoginUserInfo),
                List.of(userInfoKey+id,userRefreshTokenKey+id)
                ,member,refreshToken,String.valueOf(TimeUnit.DAYS.toSeconds(30L)));
    }
    public void logOutUserInfo(Long id){
        stringRedisTemplate.execute(new DefaultRedisScript<>(RedisLuaScript.logOutUserInfo),
                List.of(userInfoKey+id,userRefreshTokenKey+id));
    }
    public void setRedisUserInfo(Long id,String member){
        String key=userInfoKey+id;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(key, member, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, member,TimeUnit.DAYS.toSeconds(30L),TimeUnit.SECONDS);
        }
    }

    public String getUserInfo(Long id){
        return redisTemplate.opsForValue().get(userInfoKey+id);
    }

    public void saveRefreshToken(Long memberId,String refreshToken){
        redisTemplate.opsForValue().set(userRefreshTokenKey+memberId,refreshToken,30,TimeUnit.DAYS);
    }
    public Boolean existRefreshToken(Long memberId){
        return  redisTemplate.opsForValue().get(userRefreshTokenKey+memberId)!=null;
    }


}
