package com.sweep.project.redis;

public class RedisLuaScript {



    public final static String setLoginUserInfo="redis.call(\"set\", KEYS[1], ARGV[1], \"EX\",ARGV[3])\n" +
            "redis.call(\"set\", KEYS[2], ARGV[2], \"EX\", ARGV[3])\n" +
            "\n";

    public final static String logOutUserInfo="redis.call(\"del\", KEYS[1],KEYS[2])\n";
}
