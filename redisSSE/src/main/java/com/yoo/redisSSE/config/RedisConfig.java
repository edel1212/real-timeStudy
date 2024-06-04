package com.yoo.redisSSE.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper objectMapper) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // Java Object 직렬화를 위함
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // 커넥션 설정
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        /**
         * ℹ️ json 형식으로 데이터를 받을 때 값이 깨지지 않도록 직렬화한다.
         *    저장할 클래스가 여러개일 경우 범용 JacksonSerializer인 GenericJackson2JsonRedisSerializer를 이용한다
         *    setKeySerializer, setValueSerializer 설정해주는 이유는 RedisTemplate를 사용할 때
         *    Spring - Redis 간 데이터 직렬화, 역직렬화 시 사용하는 방식이 Jdk 직렬화 방식이기 때문입니다.
         *    동작에는 문제가 없지만 redis-cli을 통해 직접 데이터를 보려고 할 때 알아볼 수 없는 형태로
         *    출력되기 때문에 적용한 설정입니다.
         * */
        // 일반적인 key:value의 경우 시리얼라이저
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        // Hash를 사용할 경우 시리얼라이저
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        // 모든 경우
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.setDefaultSerializer(serializer);
        // transaction 사용
        redisTemplate.setEnableTransactionSupport(true);

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, SseEmitter> sseEmitterRedisTemplate() {
        RedisTemplate<String, SseEmitter> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    /**
     * 메시지가 도착하면 등록된 MessageListener를 호출하여 메시지를 처리한다.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        final RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        return redisMessageListenerContainer;
    }

}
