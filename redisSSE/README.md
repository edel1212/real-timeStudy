# Redis를 활용한 SSE

- 단일서버일 경우에는 문제가 없지만 서버를 `Scale-out` 할 때 문제가 발생
  -  각각의 서버인 A와 B의 접속 정보 SseEmitter가 서버 메모리에 저장되어 있기 때문이다.
-  Redis의 `pub/sub`을 사용해서 해당 문제를 해결 가능하다.
  - 각각의 n개의 서버의 구독 되어 있는 곳을 Redis로 향하게 하여 처리 하는 방식


### Redis 의존성 주입
```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### Redis 설정
- #### application.yml
  - 해당 예제에서는 `host, port`만 설정하였으나 계정 정보 및 비밀번호도 설정 가능함
- #### RedisConfig
  - ℹ️ 중요 포인트
    - Spring - Redis 간 데이터 `직렬화, 역직렬화` 시 사용하는 방식이 다르로 꼭 설정해 줘야한다.
    - DTO Class를 Redis에 저장하려면 일반적인 Google링 직렬화 역직렬화 방식만으로는 부족하다
      - `GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);` 추가 필요
    - `RedisMessageListenerContainer`설정 또한 필요하다.
      - 메시지가 도착하면 등록된 MessageListener를 호출하여 메시지를 처리한다.  
  ```java
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
  ```     
        
