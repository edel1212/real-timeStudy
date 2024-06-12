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
    - 방을 생성할떄 생성된 `UUID`를 통해 Prefix를 붙여 Redis에서 사용할 `Topik`을 만듬
    ```java
    @Log4j2
    @Repository
    @RequiredArgsConstructor
    public class ChatRoomRepository {
    
        @Value("${redis.ssePrefix}")
        private String channelPrefix;
    
        // DB 대신 사용 중
        private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
        private final RedisMessageListenerContainer container;
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
            // 3 . TODO DB 대신 목록 생성을 위한 Map임 ::: 실제 로직 경우 dummy.save(~); 구현
            chatRoomMap.put(roomId, chatRoom);
            // 4 . Redis에 저장할 Topic명(식별키) 생성
            ChannelTopic channelTopic = ChannelTopic.of( channelPrefix + roomId);
            // 5 . 구독
            container.addMessageListener(subscriber, channelTopic);
    
            return chatRoom;
        }
    }
    ```
