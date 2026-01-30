package com.strategist.ai.redis;

import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisAgentConfig {

    // 1. 配置 RedissonClient 连接本地 Redis
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379"); // 默认端口
        return Redisson.create(config);
    }

    // 2. 配置 RedisSaver
    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        // 使用默认构造函数或 Builder 模式
        return new RedisSaver.Builder().redisson(redissonClient).build();
    }

}
