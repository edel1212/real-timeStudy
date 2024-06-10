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

- #### 의존설 설정
  ```groovy
  dependencies {
      implementation 'org.springframework.boot:spring-boot-starter-websocket'
  }
  ```

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

- #### 연결 (Hand-Shake)
  - 소켓 설정이 필요하기에 `WebSocketMessageBrokerConfigurer`를 구현할 클래스를 만들어주자.
    - WebSocket STOMP end-point를 등록하기 위해 `void registerStompEndpoints(~)`를 구현
      - 해당 end-point로 연결을 요청한다.
  - ℹ️ 중요
    - 웹 소켓 활성화를 위한 `@EnableWebSocketMessageBroker` 지정
    - 설정 파일이므로 `@Configuration` 지정
    - `setAllowedOriginPatterns("*")` 설정을 통해 CORS 방지가 필요하다.
      - `setAllowedOrigins()`를 사용할 경우 배열 형태로 지정 가능 단! **"*"** 가 사용이 불가능함
        - `//.setAllowedOrigins("http://localhost:8080", "http://localhost:8081", "http://127.0.0.1:5500")`
    - PostMan을 통해 Connection 확인이 필요할 경우 `withSockJS()` 옵션을 제거 해야함 
      - `//.withSockJS()` 
      - 😱 Javascript를 통해 연결 할 경우에는 `withSockJS()`가 없으면 CORS 에러가 발생 이상하다 ..
  - 설정 파일
    - `WebSocketMessageBrokerConfigurer`를 구현한 Class
      - 구현이 강제되는 메서드는 없다. 
        - 전부 `default method`로 구현되어 있음
    ```java
    @Configuration
    //  웹소켓 활성화
    @EnableWebSocketMessageBroker
    public class WebSockConfig implements WebSocketMessageBrokerConfigurer {
        /**
         * WebSocket STOMP end-point 지정
         * */
        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            // 이 URL로 WebSocket 연결을 시작하게 됩니다
            // ws://<서버 주소>/ws-stomp로 WebSocket 연결을 시도할 수 있습니다.
            registry.addEndpoint("/ws-stomp")
                    // CORS(Cross-Origin Resource Sharing) 정책을 설정합니다
                    .setAllowedOriginPatterns("*")
                    // WebSocket을 지원하지 않는 브라우저에서도 STOMP 프로토콜을 사용할 수 있도록 SockJS 폴백(fallback) 옵션을 활성화합니다.
                    .withSockJS()
            ;
        }
    }
    ```
  - Client 
    - Javascript 기반으로 테스트 함
  ```html
  <script>
      const socket = new SockJS("http://localhost:8080/ws-stomp");
      const stompClient = Stomp.over(socket);
  </script>
  ```

- #### 구독 요청
  - ℹ️ 주의
    - `연결 (Hand-Shake)`이 성공된 상태에서 진행 되어야한다.
    - CORS 설정이 완료 되어야한다.
  - 설정 파일
    ```java
    @Configuration
    //  웹소켓 활성화
    @EnableWebSocketMessageBroker
    public class WebSockConfig implements WebSocketMessageBrokerConfigurer {
       /**
       * WebSocket 메시징을 설정할 때 호출됩니다. 주로 메시지 브로커의 설정을 담당
       * */
      @Override
      public void configureMessageBroker(MessageBrokerRegistry registry) {
          //  메모리 기반 메시지 브로커를 활성화
          // /sub로 시작하는 목적지로 클라이언트가 구독한 경우 이 브로커가 메시지를 처리합니다.
          registry.enableSimpleBroker("/sub");
      }
    }
    ```
  - Client    
    - 내장 `subscribe()`를 사용
      - 인자값 순서대로
        - 1 . 목적지 : `/sub/**/` 와 같이 상단에 설정한 브로커 주소로 시작되게 지정한다.
          - 해당 ID를 통해 방을 만들고 B,C,... 와 같은 사용자하 해당 식별키를 통해 방에 접속한다.
        - 2 . `convertAndSend()`를 통해 넘어온 값을 처리하는 로직이다
          - 쉽게 설명하면 구독 후 메세지가 왔을 경우 처리할 비즈니스 로직이다.
    ```html
    <script>
        const socket = new SockJS("http://localhost:8080/ws-stomp");
        const stompClient = Stomp.over(socket);
       /**
        * StompClient 내 함수를 통해 연결 요청
        **/
        stompClient.connect({}, function (frame) {
          console.log("Connected: " + frame);
          let currentRoomId = roomId; // 식별할 ID를 지정
          /** 
          * ✅ 구독 요청
          *    - /sub 로 시작하는 Path가 포인트이다.
          *       - 식별키 로 지정된 목적지를 통해 메세지를 주고 받는다 
          */
          stompClient.subscribe("/sub/chat/room/" + roomId, function (message) {
           
            showMessage(JSON.parse(message.body));
          });
        });
       
        /**
        * 전달 받은 JSON 형식의 데이터를 UI에 표출
        **/
        function showMessage(message) {
           const messagesDiv = document.getElementById("chatMessages");
           const messageElement = document.createElement("div");
           messageElement.className = "chat-message";
           messageElement.textContent = message.sender + ": " + message.message;
           messagesDiv.appendChild(messageElement);
           messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }
    </script>
    ```    
- #### 발행 (메세지 발행)
  - 설정 파일
    ```java
    @Configuration
    //  웹소켓 활성화
    @EnableWebSocketMessageBroker
    public class WebSockConfig implements WebSocketMessageBrokerConfigurer {
       /**
       * WebSocket 메시징을 설정할 때 호출됩니다. 주로 메시지 브로커의 설정을 담당
       * */
      @Override
      public void configureMessageBroker(MessageBrokerRegistry registry) {
         // 애플리케이션에서 메시지를 보내기 위한 목적지 경로의 접두사를 설정합니다.
         // 서버는 이 접두사를 보고 해당 메시지를 애플리케이션의 메시지 처리기로 라우팅합니다.
         registry.setApplicationDestinationPrefixes("/pub");
      }
    }
    ``` 
  - Client
    - 내장 `send()`를 사용
      - 인자값 순서대로
        - 1 . 발행 주소 : `/pub/{{내가 구현한 MessageMapping 주소}}/`로 지정 
          - 👉 위에 설정한 `/pub/**`로 Path가 구성되는 것이 포인트다!  
          - 해당 Controller를 통해 내부 로직의 메세지 발송(`convertAndSend()`)을 통해 발송 한다.
        - 2 . `Header` 정보 등록
          - 필수 값이 아니다 `{}`로 해서 보내도 문제 없으나 권한 체크용도로 사용 가능
        - 3 . Message 내용
    ```html
    <script>
        function sendMessage() {
        const message = document.getElementById("messageInput").value;
        const nickname = document.getElementById("nicknameInput").value;
          const body = {
            roomId: currentRoomId,
            sender: nickname, 
            message,
          };
          stompClient.send(
            // ✅  발행 주소 
            "/pub/chat/message"
            // ✅  Header
            , { "Authorization": "Bearer some_dummy_token" }
            // ✅ Body(Message) 내용
            , JSON.stringify(body)
          );
          document.getElementById("messageInput").value = "";
        } // func
    </script>
    ```    
  - Controller
    - `@MessageMapping("~")`를 통해 다양한 Mapping을 추가할 수 있다. 
    - 앞에 설정한 prefix를 붙여야 해당 MessageMapping에 접근이 가능하다.
    - 실제 메세지가 보내지는 핵심 코드는 `SimpMessageSendingOperations 내 convertAndSend()`메서드 이다.
      - 인자값
        - 1 . 식별키
        - 2 . 전달 하고자하는 메세지
    - `@SendTo()`를 사용하면 다른 구독 자들에게도 똑같은 메세지가 전송된다.
    ```java
    @Log4j2
    @RequiredArgsConstructor
    @Controller
    public class ChatController {
        private final SimpMessageSendingOperations messagingTemplate;
    
        /**
         * ✅ MessageMapping 어노테이션을 사용한다.
         * - 입장, 글쓰기 모두 이곳을 통해 전달 된다.
         * - 앞에 설정한 prefix가 붙어야 전달된다. {@link com.yoo.simple.WebSocket.config.WebSockConfig }
         *
         * */
        @MessageMapping("/chat/message")
        //@SendTo("구독 주소") // ℹ️ 해당 메서드를 타면 지정 구독자들에게 같은 메세지가 전달 가능하다.
        public void message(ChatMessage message,
                            @Header("Authorization") String authHeader,
                            @Headers Map<String, Object> headers) {
            // 개별 헤더 값 출력
            log.info("------------");
            log.info("Authorization Header ::: {}", authHeader);
            log.info("message ::: {}", message);
            log.info("------------");
    
            // 전체 헤더 출력
            log.info("Headers ::: {}", headers);
    
            // 👍 실제 메시지 처리 로직
            messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
        }
    }
    ```
