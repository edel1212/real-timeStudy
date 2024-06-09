# STOMP(Simple Text Oriented Messaging Protocol)

- WebSocket과 같은 OSI 7계층 위에서 작동하는 메시징 프로토콜입니다. WebSocket이 연결된 후, STOMP는 메시지의 전송(pub), 구독()sub 및 라우팅을 처리하는데 필요한 추가 기능을 제공합니다.
  - 메시징 패턴(Pub/Sub, Point-to-Point) 지원.
  - 메시징 패턴을 사용하므로써 다수의 채팅방을 만드는 기능 또한 사용 가능하다.
    - 예제에서는 사용하지 않았으나 `Redis`를 사용하여 Scale-out 가능
- 메세지의 헤더에 값을 줄 수 있어 헤더 값을 기반으로 통신 시 인증 처리를 구현하는 것도 가능하며 STOMP 스펙에 정의한 규칙만 잘 지키면 여러 언어 및 플랫폼 간 메세지를 상호 운영할 수 있다
- Text 지향 프로토콜이나, Message Payload에는 Text or Binary 데이터를 포함 할 수 있다.
- STOMP 프레임워크를 지원하는 라이브러리나 서버가 필요함.


### WebSocket과 STOMP 비교

| **기능**                 | **WebSocket**                          | **STOMP**                                      |
|--------------------------|-----------------------------------------|------------------------------------------------|
| **레벨**                 | 저수준 통신 프로토콜                   | 고수준 메시징 프로토콜                         |
| **통신 형태**            | 양방향 실시간 통신                     | 양방향 실시간 통신 + 메시징 패턴 지원          |
| **메시지 라우팅**         | 수동으로 구현해야 함                    | 목적지 기반의 자동 라우팅                      |
| **메시지 형식**           | 바이너리 또는 텍스트                   | 텍스트 기반 (프레임 구조)                      |
| **구독 및 발행**          | 직접 구현 필요                         | 내장된 구독 및 발행 지원                      |
| **구조적 메시지**         | 없음                                   | 메시지 헤더 및 바디 지원                       |
| **연결 및 재시도 관리**    | 직접 구현 필요                         | 프레임워크에서 기본 제공                      |
| **디버깅 및 가독성**       | 구현에 따라 다름                       | 텍스트 기반으로 디버깅 및 가독성 우수          |
| **오버헤드**              | 낮음                                   | 약간의 오버헤드 추가                          |


### 흐름별 코드

- #### 방 생성
  - Controller 
    - 간단한 예제로 제작 하였기에 `@PathVariable`사용
    - 소켓 기능이 아닌 `HttpMethod`이다.
    - 해당 생성된 방의 `UUID`를 활용하여 STOMP의 `식별키`로 활용함

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
    - 간단한 예제로 제작 하였기에 Service 스킵
    - Thread-Safety한 `new ConcurrentHashMap()` 자료 구조 사용
      - 메모리를 사용하기에 확장성은 떨어진다.
    ```java
    @Log4j2
    @Repository
    @RequiredArgsConstructor
    public class ChatRoomRepository {
        private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap();
  
        // 채팅방 생성 - 주식별키는 UUID로 생성함
        public ChatRoom createChatRoom(String roomName) {
            String roomId = UUID.randomUUID().toString();
            log.info("-----------");
            log.info("roomId ::: {}",roomId);
            log.info("-----------");
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomId(roomId)
                    .name(roomName)
                    .build();
            chatRoomMap.put(roomId, chatRoom);
            return chatRoom;
        }
  
    }
  
    /** ------------------------------------------------------------------------------------ **/
  
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public class ChatRoom {
      private String roomId;
      private String name;
    }
    ```
  - Client - 화면
    - 간단한 POST 요청으로 제외함 

- #### 조회
  - 단순 코드기에 Skip 필요 시 Git 코드 확인