package com.groom.e_commerce.globalv2.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
@Configuration
public class RedisConfig {

    /**
     * 범용 RedisTemplate
     * - 캐시
     * - 이벤트 중복 방지
     * - 임시 데이터
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory factory,
        StringRedisSerializer stringSerializer,
        GenericJackson2JsonRedisSerializer jsonSerializer
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    /**
     * Pub/Sub, Lock, Counter 전용
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(
        RedisConnectionFactory factory
    ) {
        return new StringRedisTemplate(factory);
    }
}
