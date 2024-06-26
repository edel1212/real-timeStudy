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

### Scale Out
```properties
## 서버가 끊길 경우 Client 측에서 재전송이 필요하다.
## 그렇게되면 분산처리가 되어있을 경우 다른 서버로 이동 되어 접근되며
## 새로이 구독 되어있는 대상에 Sub되어 분산 처리가 가능하다 
```


### Redis 설정
- #### application.yml
  - 해당 예제에서는 `host, port`만 설정하였으나 계정 정보 및 비밀번호도 설정 가능함
```properties
spring:
  data:
    redis:
      host: localhost
      port: 6379
``` 
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

### 흐름 코드 - 생성(구독)
```properties
# 편의를 위해 Interface를 제외하고 구현하였음
# 사용된 Redis는 기본 포트를 적용함
```

- Controller
  - ℹ️ 중요 
    - 구독 시 응답 유형은 `SseEmitter`로 반환하며 제공 Header-Type은 `MediaType.TEXT_EVENT_STREAM_VALUE`이다.
    - `HttpMethod`의 형식은 반드시 Get 방식이어야 한다.
  ```java
  @RestController
  @RequiredArgsConstructor
  public class NotificationController {
  
      private final NotificationServiceImpl notificationService;
  
      @GetMapping(value = "/sub", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
      public SseEmitter subscribe(String channel){
        return notificationService.subscribe(channel);
      }
  
  }
  ```

- Service - 알림 연결 생성 
  - ℹ️ 중요
    - `SseEmitter` 생성 시 반드 시 공백이라도 메세지를 한개라도 보내줘야함.
      - 그렇지 않으면 연결이 성사 되지 않음
  ```java
  @Service
  @Log4j2
  @RequiredArgsConstructor
  public class NotificationServiceImpl {
      private final SseEmitterService sseEmitterService;
      private final RedisMessageService redisMessageService;
  
      public SseEmitter subscribe(String channel) {
          // 1 . SSE 객체 생성
          SseEmitter sseEmitter = sseEmitterService.createSseEmitter(channel);
  
          // 2 . 메세지 전송 최초 1회 필수
          NotificationDto data = NotificationDto.builder()
                  .channel(channel)
                  .message("Create Channel Id : " + channel)
                  .build();
          sseEmitterService.sendMessage(data, sseEmitter);
  
          // 3 . Redis 구독
          redisMessageService.subscribe(channel);
  
          // 4 . SSE 성공 및 실패 처리
          sseEmitter.onTimeout(sseEmitter::complete);
          sseEmitter.onError((e) -> sseEmitter.complete());
          sseEmitter.onCompletion(() -> {
              // Map에 저장된 sseEmitter 삭제
              sseEmitterService.removeChannel(channel);
              // 구독한 채널 삭제
              redisMessageService.removeSubscribe(channel);
          });
          return sseEmitter;
      }
  }    
  ```

- Service - 1 . SSE 객체 생성
  - SSE 연결을 위한 `Sseemitter` 객체 생성

  ```java
  @Log4j2
  @RequiredArgsConstructor
  @Service
  public class SseEmitterService {
  
      private final SseEmitterRepository sseEmitterRepository;
  
      public SseEmitter createSseEmitter(String channel) {
          return sseEmitterRepository.save(channel);
      }
  }    
  
  /*** =============================================================================  **/
  
  @Repository
  public class SseEmitterRepository {
    // thread-safe한 자료구조를 사용한다.
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private Long timeout = 60L * 1000 * 60;
  
    public SseEmitter save(String eventId) {
      SseEmitter sseEmitter =  new SseEmitter(timeout);
      emitters.put(eventId, sseEmitter);
      return sseEmitter;
    }
    
  }
  ```

- Service - 2 . 메세지 전송 최초 1회 필수

  ```java
  @Log4j2
  @RequiredArgsConstructor
  @Service
  public class SseEmitterService {
      public void sendMessage(NotificationDto data, SseEmitter sseEmitter) {
          log.info("send to client :[{}]",  data);
          String channel =  data.getChannel();
          try {
              sseEmitter.send(SseEmitter.event()
                      .id(channel)
                      .name("sse")
                      .data(data, MediaType.APPLICATION_JSON));
          } catch (IOException | IllegalStateException e) {
              log.error("IOException | IllegalStateException is occurred. ", e);
              // 에러가 발생할 경우 채널 삭제
              sseEmitterRepository.deleteById(channel);
          } // try - catch
      }
  }
  ```

- Service - 3 . Redis 구독
  - ℹ️ 중요
    - `MessageListener`를 관리하고 조정하여, Redis 채널이나 패턴에 게시된 메시지에 반응하게 하는 객체 주입 필수
      - 해당 객체에 등록할 `MessageListner`(인터페이스)를 구현한 구현체 주입 필수
        - 해당 예제에서는 `RedisSubscriber`를 구현하여 사용함
    - 채널 이름에 사용할 Prefix 지정 확정성을 위함
      - 만약 채널명을 계정명으로하면 만약 SSE 와 웹 소켓을 사용할 경우 중복될 수 도 있기 때문이다.
    ```java
    @RequiredArgsConstructor
    @Service
    public class RedisMessageService {
    // 채널에 사용할 Prefix - 강제는 아니다 SSE 이외 다양하게 Redis 구독을 사용하기 위함
    @Value("${redis.ssePrefix}")
    private String channelPrefix;
    /**
    * ℹ️ 메시지 리스너를 관리하고 조정하여, Redis 채널이나 패턴에 게시된 메시지에 반응할 수 있도록 함
    * - MessageListener 구현체를 등록하여, 들어오는 메시지를 애플리케이션이 어떻게 처리할지 정의합니다.
    * - 컨테이너는 리스너를 관리하고, 메시지가 도착하면 이를 적절한 리스너에게 전달합니다.
    * */
    private final RedisMessageListenerContainer container;
    /**
    * ℹ️ 메세지를 수신 및 처리를 담당
    * - RedisMessageListenerContainer에 주입될 구현체이다.
    *    - 해당 Class에 정의된 내용대로 메세지를 처리한다.
    * */
    private final RedisSubscriber subscriber;
    private final RedisTemplate<String, Object> redisTemplate;
    
        /**
         * 채널 구독
         * */
        public void subscribe(String channel) {
            //  Redis의 지정 채널에 메시지가 게시될 때마다 MessageListener를 구현한 Class가 해당 메시지를 처리함
            container.addMessageListener(subscriber, ChannelTopic.of(getChannelName(channel)));
        }
    
        private String getChannelName(String id) {
            return channelPrefix + id;
        }
    }
    ```
    
- Service - 3.1 . `MessageLisner` 구현체
  - `MessageListener`는 `@FunctionalInterface`이다.
    - `public void onMessage(Message message, byte[] pattern)`메서드 구현이 강제된다.
  - 첫 구독에는 굳이 Client가 없기에 `sendNotificationToClient()`가 불필요하다 생각하겠지만 해당 리스너를 통해 이후에도 메세지를  전달할 경우 사용한다는 것을 기억하자
  - 채널명 앞에 지정된 값을 넣어 확장성을 높임
  - 🤣 삽질 내용
    - `SseEmitterService`를 의존성 주입하지 않고 `NotificationServiceImpl`를 활용해서 구현하려 했다.
      - Spring Cycle 에러 발생 .. 잘 생각해보면 당연한 결과였다 . 부르고 -> 구독 로직 -> 부른 곳 다시 호출 ..

    ```java
    @Log4j2
    @RequiredArgsConstructor
    @Component
    public class RedisSubscriber implements MessageListener {
    
        private final ObjectMapper objectMapper;
        private final SseEmitterService sseEmitterService;
        @Value("${redis.ssePrefix}")
        private String channelPrefix;
    
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                // Redis에서 저장된 Key(채널)값은 Prefix를 달아 저장했기에
                // 해당 Prefix를 제거한 후 Map에 저장된 SS
                String channel = new String(message.getChannel())
                        .substring(channelPrefix.length());
    
                log.info("channel ::: " + channel);
                log.info("message ::: " + message.getBody().toString());
    
                NotificationDto notificationDto = objectMapper.readValue(message.getBody(),
                        NotificationDto.class);
                // 구독하고 있는 Client들에게 메세지를 전달한다.
                sseEmitterService.sendNotificationToClient(notificationDto);
            } catch (IOException e) {
                log.error("IOException is occurred. ", e);
            }
        }
    }
    ```

- Service - 3.2 . 구독하고 있는 Client 에게 메세지 전달을 위함
  ```java
  @Log4j2
  @RequiredArgsConstructor
  @Service
  public class SseEmitterService {
  
      private final SseEmitterRepository sseEmitterRepository;
  
      public void sendNotificationToClient(NotificationDto notificationDto) {
          String accountId = notificationDto.getChannel();
          Optional<SseEmitter> optionalSseEmitter = sseEmitterRepository.findById(accountId);
          // 해당 Map에서 SseEmitter가 없을 경우 예외 처리
          if (!optionalSseEmitter.isPresent()) return;
          // 메세지 전송
          this.sendMessage(notificationDto, optionalSseEmitter.get());
      }
  }   
  ```

- Service - 3.3 . `SseEmitter` 객체 존제 확인
  ```java
  @Repository
  public class SseEmitterRepository {
      // thread-safe한 자료구조를 사용한다.
      private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
      public Optional<SseEmitter> findById(String memberId) {
          return Optional.ofNullable(emitters.get(memberId));
      }
  }
  ```
  
- Service - 4 . SSE 성공 및 실패 처리
  - 기존 Redis의 저장 내용과 Map에 저장되어 있는 내용을 삭제 하는 간단한 로직이기에 스킵함
    - 필요할 경우 Git 확인

### 흐름 코드 - 메세지 전송(퍼블리싱)

- Controller

  ```java
  @RestController
  @RequiredArgsConstructor
  public class NotificationController {
      private final NotificationServiceImpl notificationService;
  
      @PostMapping(value = "/send-data", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
      public void sendData(String channel, String message) {
          notificationService.sendNotification(channel, message);
      }
  }
  ```
  
- Service - 1 . Redis Publishing

  ```java
  @Service
  @Log4j2
  @RequiredArgsConstructor
  public class NotificationServiceImpl{
      private final RedisMessageService redisMessageService;
  
      public void sendNotification(String channel, String message) {
          NotificationDto data = NotificationDto.builder()
                  .channel(channel)
                  .message(message)
                  .build();
          // redis 이벤트 발행
          redisMessageService.publish(channel, data);
      }
  }
  ```

- Service - 2 . 구독 식별키에 맞춰 발송
  - 추가 설명 어째서 `convertAndSend(식별키, 전송 데이터);`만으로 알림이 전송 가능 이유
    - `MessageListener`를 구현한 `void onMessage()`메서드안에서 해당 Key를 통해 SSeEmitter를 구하는 로직이 있기 때문이다.
      - 그럼 왜? Redis에 **SSE객체를 저장**하면 되잖아? 라면 Redis에 **너무 복잡한 Object를 저장할 수 없기** 때문이다.

  ```java
  @RequiredArgsConstructor
  @Service
  public class RedisMessageService {
      // 채널에 사용할 Prefix - 강제는 아니다 SSE 이외 다양하게 RedisMessageListenerContainer를 사용하기 위함
      @Value("${redis.ssePrefix}")
      private String channelPrefix;
      
      private final RedisTemplate<String, Object> redisTemplate;
  
      /**
       * Reids에 저장된 채널에 이벤트 발행
       * */
      public void publish(String channel, NotificationDto notificationDto) {
          redisTemplate.convertAndSend(getChannelName(channel), notificationDto);
      }
  
      private String getChannelName(String id) {
          return channelPrefix + id;
      }
  }
  ```

- etc - 흐름 메세지 전송 로직 도움 코드

  ```java
  public class RedisMessageService {
      private final RedisSubscriber subscriber;
      // Code...
      public void subscribe(String channel) {
          // ℹ️ 구독 시 주입하는 subscriber(MessageListener)가 포인트다
          container.addMessageListener(subscriber, ChannelTopic.of(getChannelName(channel)));
      }
  }
  
  /**************************************************************************************/
  
  public class RedisSubscriber implements MessageListener {
    // Code...
    private final SseEmitterService sseEmitterService;
  
    @Override
    public void onMessage(Message message, byte[] pattern) {
      try {
  
        // ℹ️ 포인트
        sseEmitterService.sendNotificationToClient(notificationDto);
      } catch (IOException e) { }
    }
  }
  
  /**************************************************************************************/
  
  public class SseEmitterService {
      // Code...
      private final SseEmitterRepository sseEmitterRepository;
  
      public void sendNotificationToClient(NotificationDto notificationDto) {
          // 1 . 받아온 데이터 기준으로 채널명을 가져옴
          String accountId = notificationDto.getChannel();
          // 2 . 저장되어있는 SSE 객체를 가져온다  << 해당 코드를 통해 SSE객체를 가져올 수 있었던 것이다
          Optional<SseEmitter> optionalSseEmitter = sseEmitterRepository.findById(accountId);
          // 3 . 해당 Map에서 SseEmitter가 없을 경우 예외 처리
          if (!optionalSseEmitter.isPresent()) return;
          // 4 . 메세지 전송
          this.sendMessage(notificationDto, optionalSseEmitter.get());
      }
  }  
  ```

### 간단 요약
- 1 . 사용자가 `식별키`를 지정해 서버에 SSE 접속을 요청함
  - 응답 Header는 반드시 `TEXT_EVENT_STREAM_VALUE`!
- 2 . 서버는 `식별키`를 이용해 SSE 객체를 생성함
  - 반드시 메세지를 1회 전송해야 접속이 허용됨
  - Redis에 해당 `prefix + 식별키`를 통해 `topic`을 만든 후 구독
    - ℹ️ 중요 포인트) 구독 시 실행 메서드를 주입하여 SSE 연결된 사용자에게 메세지 전달하게 끔 함
      - `sseEmitter.send()` 메서드를 Pub시 응답 메서드에 심어 놓음
- 3 . 서버에서 SSE 연결된 사용자에게 메세지 전달 
  - `redisTemplate.convertAndSend( 토픽명 , 메세지 내용 );`를 통해 전달
    - ℹ️ 중요 포인트) `(2)`에서 기술한 내부 심어 놓은 `sseEmitter.send()`를 통해 메세지를 전달 받을 수 있음
