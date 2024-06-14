# Redis With WebSocket(STOMP) 

```properties
# 이전 WbSocekt 부분과 중복된는 설정은 제외함
# 서술 제외 Class [ WebConfig, WebSockConfig, build.gradle ]
# 방 목록은 InMemory로 구현하였으나 고도화 경우 DB를 사용하여 구현하자
```

### Redis 설정

- [참고](https://github.com/edel1212/real-timeStudy/tree/main/redisSSE)
  - 이전 Redis를 활용한 SSE에서 사용했던 Redis 설정을 그대로 사용하였다.

### 흐름 코드

- #### 방 생성
  - ℹ️ 해당 부분에서 삽질함
    - 이미 만들어진 방에서 3Way-Hand-Shake 시 구독을 하려고 애를 썼지만 비효율 적이며 로직상 이상함을 확인
      - 방 생성시 생성되는 해당 방의 `식별키`를 이용해서 Redis의 `Topic`으로 지정하여 구독을 진행
  - Controller
    ```java
    @Log4j2
    @RequiredArgsConstructor
    @RestController
    @RequestMapping("/chat")
    public class ChatRoomController {
    
        private final ChatRoomRepository chatRoomRepository;
    
        // 채팅방 생성
        @PostMapping(value = "/room/{roomName}")
        public ChatRoom createRoom(@PathVariable String roomName) {
            return chatRoomRepository.createChatRoom(roomName);
        }
    
    }
    ```
    
  - Repository
    - 방을 생성할떄 생성된 `UUID`를 통해 Prefix를 붙여 Redis에서 사용할 `Topic`을 만듬
      - `ChannelTopic.of( 토픽명 )`
    - `container.addMessageListener( 리스너 구현 Class , 토픽명 )`을 통해 구독 
    ```java
    @Log4j2
    @Repository
    @RequiredArgsConstructor
    public class ChatRoomRepository {
    
        @Value("${redis.ssePrefix}")
        private String channelPrefix;
    
        // DB 대신 사용 중
        private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
        // ℹ️ 구독을 등록할 컨테이너 
        private final RedisMessageListenerContainer container;
        // ℹ️ 컨테이너에 들어갈 메세지리스너 구현체
        private final RedisSubscriber subscriber;
    
        /**
         * 채팅방 생성
         *  - redis에 구독
         */
        public ChatRoom createChatRoom(String roomName) {
            // 1 . UUID 생성 
            String roomId = UUID.randomUUID().toString();
            // 2 . 메세지 생성
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomId(roomId)
                    .name(roomName)
                    .build();
            // 3 . TODO DB 대신 목록 생성을 위한 Map임 ::: 실제 로직 경우 repository.save(~); 구현
            chatRoomMap.put(roomId, chatRoom);
            // 4 . Redis에 저장할 Topic명(식별키) 생성
            ChannelTopic channelTopic = ChannelTopic.of( channelPrefix + roomId);
            // 5 . 구독
            container.addMessageListener(subscriber, channelTopic);
    
            return chatRoom;
        }
    }
    ```
  - RedisSubscriber (`MessageListener` 구현 Class)
    - `MessageListener`는 @FunctionalInterface이다.
      - 람다식으로 표현이 가능 하지만 로직이 복잡할 경우 예시처럼 구현을 통해 사용해주자
    - `SimpMessageSendingOperations` 의존성 주입을 통해 pub요청이 올 경우 메세지가 전송 되게 끔 해주자 
      -  `messagingTemplate.convertAndSend(  STOMP지정Path  +  토픽명  , chatMessage);`
    ```java
    @Log4j2
    @RequiredArgsConstructor
    @Component
    public class RedisSubscriber implements MessageListener {
        private final ObjectMapper objectMapper;
        // ℹ️ STOMP 주입 (메세지를 전송하기 위한 의존성 주입)
        private final SimpMessageSendingOperations messagingTemplate;
        @Value("${redis.ssePrefix}")
        private String channelPrefix;
    
        /**
         * 구독자들에게 <pre>convertAndSend() 시</pre> 사용 될 메서드이다
         * */
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                // 1 . Redis에서만 사용했던 prefix 제거
                String roomId = new String(message.getChannel())
                        .substring(channelPrefix.length());
    
                log.info("channel ::: {}  ",roomId);
                
                // 2 . RedisValue -> JavaObject 변환
                ChatMessage chatMessage = objectMapper.readValue(message.getBody(),
                        ChatMessage.class);
    
                // 3 . 구독자들에게 Stomp를 사용해서 메세지 전송
                messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, chatMessage);
    
            } catch (IOException e) {
                log.error("IOException is occurred. ", e);
            } // try - catch
        }
    }
    ```

- #### 방 참가 - 구독
  - Client
    - 아래의 연결이 이해가 안 간다면 `WebSockConfig` Class를 참고하자
    ```html
    <script>
      // 연결 요청
      const socket = new SockJS("http://localhost:8080/ws-stomp");
      stompClient = Stomp.over(socket);
    
      // 지정 Path에 맞게 커넥션 요청
      stompClient.connect({}, function (frame) {
        console.log("Connected: " + frame);
        currentRoomId = roomId;
        // ✅ 구독 요청
        stompClient.subscribe("/sub/chat/room/" + roomId, function (message) {
          showMessage(JSON.parse(message.body));
        });
      });
    </script>
    ```

- #### 메세지 전송
  - Controller
    - `@MessageMapping`를 통해 Url을 Mapping한다
      - 포인트는 `WebSockConfig`에서 설정한 목적지(`DestinationPrefixes`)로 연결된다는 점이다
        - ex) `registry.setApplicationDestinationPrefixes("/pub");`
    - 입장한 방의 RoomId에 prefix를 붙여 Topic을 만들어 Redis에 메세지를 전송시키는 방식이다
    ```java
    @Log4j2
    @RequiredArgsConstructor
    @Controller
    public class ChatController {
        private final RedisTemplate<String, Object> redisTemplate;
  
        @Value("${redis.ssePrefix}")
        private String channelPrefix;
  
        @MessageMapping("/chat/message")
        public void message(ChatMessage message) {
            // 1. RoomId를 가져옴
            String roomId = message.getRoomId();
            // 2 . roomId -> Redis 식별키 변경
            String topic = channelPrefix + roomId;
            // 3 . 구독자(topic)들에게 emmit
            redisTemplate.convertAndSend(topic , message);
        }
  
    }
    ```
  - Client
    ```html
    function sendMessage() {
      const message = document.getElementById("messageInput").value;
      const nickname = document.getElementById("nicknameInput").value;
      if (message && stompClient && currentRoomId) {
        const body = {
          roomId: currentRoomId,
          sender: nickname, 
          message,
        };
        stompClient.send("/pub/chat/message", {
          // ✅  여기에 권한 정보를 추가합니다.
            "Authorization": "Bearer some_dummy_token" 
          }, JSON.stringify(body));
        document.getElementById("messageInput").value = "";
      }
    }
    ```
