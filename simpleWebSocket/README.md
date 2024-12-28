# STOMP(Simple Text Oriented Messaging Protocol)

- WebSocket과 같은 OSI 7계층 위에서 작동하는 메시징 프로토콜입니다. WebSocket이 연결된 후, STOMP는 메시지의 전송(pub), 구독()sub 및 라우팅을 처리하는데 필요한 추가 기능을 제공합니다.
  - 메시징 패턴(Pub/Sub, Point-to-Point) 지원.
  - 메시징 패턴을 사용하므로써 다수의 채팅방을 만드는 기능 또한 사용 가능하다.
    - 예제에서는 사용하지 않았으나 `Redis`를 사용하여 Scale-out 가능
- 메세지의 헤더에 값을 줄 수 있어 헤더 값을 기반으로 통신 시 인증 처리를 구현하는 것도 가능하며 STOMP 스펙에 정의한 규칙만 잘 지키면 여러 언어 및 플랫폼 간 메세지를 상호 운영할 수 있다
- Text 지향 프로토콜이나, Message Payload에는 Text or Binary 데이터를 포함 할 수 있다.
- 메세징 전송을 효율적으로 하기 위해 탄생한 프로토콜
  - 기본적으로 `pub / sub 구조`이다
  - 메세지를 전송하고 메세지를 받아 처리하는 부분이 확실히 정해져 있기 때문에 개발자 입장에서 명확하게 인지하고 개발할 수 있는 이점이 있다.
- STOMP 프레임워크를 지원하는 라이브러리나 서버가 필요

## pub, sub 이란?
- sub(Subscribe) - 구독 
  - 클라이언트가 -> 특정 대상을  **구독**
- pub(Publish) - 발행 
  - 클라이언트 -> 메시지를 특정 대상에게 **메세지 진달**
- 예시
  - 채팅방을 생성한다 :: **Topic** 생성
  - 채팅방 입장 :: Topic **구독**
  - 채팅방에서 메세지 송신 :: 지정 `Topic`으로 메세지를 `송신(pub)`
  - 채팅방에서 메세지 수신 ::  구독 되어있는 `Topic`으로 메세지 받음


## WebSocket과 STOMP 비교

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


## 사용 방법

### Dependencies 
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
}
```

### WebSockConfig 설정 Class
```properties
# ℹ️ Websocket과 설정이 비슷하지만 구현에 필요한 Interface가  "WebSocketMessageBrokerConfigurer"이다
```

- 소켓 설정이 필요하기에 `WebSocketMessageBrokerConfigurer`를 interface를 구현 Class
  - 구현 Method
    - `registerStompEndpoints(MessageBrokerRegistry registry)` : endPoint 및 cors 설정
    - `configureMessageBroker(MessageBrokerRegistry registry)` : sub 및 pub path 설정
- ℹ️ 중요
  - 웹 소켓 활성화를 위한 `@EnableWebSocketMessageBroker` 지정
  - 설정 파일이므로 `@Configuration` 지정
  - `setAllowedOriginPatterns("*")` 설정을 통해 CORS 방지가 필요하다.
    - `setAllowedOrigins()`를 사용할 경우 배열 형태로 지정 가능 단! **"*"** 가 사용이 불가능함
      - `//.setAllowedOrigins("http://localhost:8080", "http://localhost:8081", "http://127.0.0.1:5500")`
  - 테스트 시
    - PostMan을 통해 Connection 확인이 필요할 경우 `withSockJS()` 옵션을 제거 해야함
      - `//.withSockJS()`
  - Javascript를 통해 연결 할 경우에는 `withSockJS()`가 없으면 CORS 에러가 발생 하므로 필수

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
        // ws://<서버 주소>/ws-stomp로 WebSocket 연결을 시도할 수 있습니다.
        registry.addEndpoint("/ws-stomp")
                // CORS(Cross-Origin Resource Sharing) 정책을 설정합니다
                .setAllowedOriginPatterns("*")
                // WebSocket을 지원하지 않는 브라우저에서도 STOMP 프로토콜을 사용할 수 있도록 SockJS 폴백(fallback) 옵션을 활성화합니다.
                .withSockJS()
        ;
    }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // Client에서 SEND 요청을 처리 - 요청 path 시작 설정
    registry.setApplicationDestinationPrefixes("/pub");
    //  해당하는 경로를 SUBSCRIBE하는 Client에게 메세지를 전달하는 간단한 작업을 수행 - 응답 path 시작 설정
    registry.enableSimpleBroker("/sub");
  }
}
```

### 방 생성 및 조회
```properties
# ℹ️ STOMP에서 사용할 간단한 방생성 API
#    - Websocket Logic과는 상관 없음
#
#   간단한 예제로 제작 하였기에 Service 제외 구현
#   - 생성된 방의 `UUID`를 활용하여 STOMP의 `식별키`로 활용
```

#### DTO
```java
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ChatRoom {
  private String roomId;
  private String name;
}
```

#### Repository
- Thread-Safety한 `new ConcurrentHashMap()` 자료 구조 사용
  - 운용 환경에서는 DB활용 필요
```java
@Log4j2
@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap();

    // 채팅방 생성 - 주식별키는 UUID로 생성함
    public ChatRoom createChatRoom(String roomName) {
        String roomId = UUID.randomUUID().toString();
        log.info("roomId ::: {}",roomId);
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(roomName)
                .build();
        chatRoomMap.put(roomId, chatRoom);
        return chatRoom;
    }

    public List<ChatRoom> findAllRooms(){
      //채팅방 생성 순서 최근 순으로 반환
      List<ChatRoom> result = new ArrayList<>(ChatRoom.values());
      Collections.reverse(result);
      return result;
    }
}
```

#### Controller
```java
@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatRoomController {
  
    private final ChatRoomRepository chatRoomRepository;

    @GetMapping("/roomList")
    @ResponseBody
    public ResponseEntity<List<ChatRoomDTO>> rooms(){
      return ResponseEntity.ok(chatRoomRepository.findAllRooms());
    }
    
    // 채팅방 생성
    @PostMapping(value = "/room/{roomName}")
    public ChatRoom createRoom(@PathVariable String roomName) {
        return chatRoomRepository.createChatRoom(roomName);
    }
}
```

### 구독 신청
```properties
# ℹ️ `연결 (Hand-Shake)`이 성공된 상태에서 진행
#
# 👉 해당 설정을 해줘야 sub, pub 시 로그기 안나옴
#     stomp.debug = null;
```
- WebSockConfig에서 설정한 `enableSimpleBroker()` 기반 Path로 구독 요청을 보내야함
  
#### Client
- Stomp 인스턴스 내 `subscribe()`를 사용
  - 인자값 순서대로
    - 첫번째 argument  [ 식별 위치 ]
      - `/sub/**/`로 시작하는 Path에 맞게 구독 
        -  /sub 뒤 Path를 통해 구분
        - ID를 통해 방을 만들고 B,C,... 와 같은 사용자하 해당 식별키를 통해 방에 접속
    - 두번째 argument [ 함수 ]
      - Server 내 `SimpMessagingTemplate`의 `convertAndSend()`를 사용해 넘어온 메세지 처리하는 로직이다
        - 쉽게 설명하면 구독 후 메세지가 왔을 경우 처리 방법
```javascript
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>

const socket = new SockJS("http://localhost:8080/ws-stomp");
const stompClient = Stomp.over(socket);
// 👉 해당 설정을 해줘야 sub, pub 시 로그기 안나옴
stomp.debug = null;

/**
* StompClient 내 함수를 통해 연결 요청
**/
stompClient.connect({}, function (frame) {
  console.log("Connected: " + frame);
  let currentRoomId = "UUID로 생성된 방 ID";
  
  /** 
  * ✅ 구독 요청
  *    - /sub 로 시작하는 Path가 포인트이다.
  *       - 식별키 로 지정된 목적지를 통해 메세지를 주고 받는다 
  *    
  * ✅ 메세지 처리 방법
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
```    

### 메세지 발행 
- WebSockConfig에서 설정한 `setApplicationDestinationPrefixes()` 기반 Path로 메세지를 발행 요청 해야함


#### Controller - 메세지를 전달 받을 MappingController
```properties
# ℹ️ ℹ️ prefix에 `WebSockConfig`에서 설정한 **발행 Path 붙여야  MessageMapping에 접근이 가능**
```
- `@MessageMapping("요청 Path")`를 통해 메세지를 받을 URL 지정
- 실제 메세지가 보내지는 핵심 인스턴스는 `SimpMessageSendingOperations`이며,  `convertAndSend()`를 통해 전달 함
  - 인자값
    - 첫번째 argument : [ 전송 하고자 하는 식별 위치 ]
    - 두번째 argument : [ 전달 하고 자하는 메세지 ]
- `@SendTo()`를 사용하면 다른 구독 자들에게도 똑같은 메세지가 전송된다.
```java
@Log4j2
@RequiredArgsConstructor
@Controller
public class ChatController {
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * ✅ 해당 Path 앞 Websocket Config에서 설정한 발행 Path를 꼭 붙여야한다. 
     * */
    @MessageMapping("/chat/message")
    //@SendTo("구독 주소") // ℹ️ 해당 메서드를 타면 지정 구독자들에게 같은 메세지가 전달 가능하다.
    public void message(ChatMessage message,
                        @Header("Authorization") String authHeader,
                        @Headers Map<String, Object> headers) {
        // 👍 실제 메시지 처리 로직
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }
}
```

#### Client
-  Stomp 인스턴스 내 `send()`를 사용
  - 인자값
    -  첫번째 argument : [ 발행 주소 ] `/pub/{{내가 구현한 MessageMapping 주소}}/`로 지정
      - 👉 위에 설정한 `/pub/**`로 **Path가 구성**되는 것이 **포인트**
    - 두번째 argument :  [ `Header` ] 정보
      - 필수 값이 아니다 `{}`로 해서 보내도 문제 없으나 권한 체크용도로 사용 가능
    - 세번째 argument : [ Message ]
```javascript
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>

const socket = new SockJS("http://localhost:8080/ws-stomp");
const stompClient = Stomp.over(socket);

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
```    

### 전체 client 

```javascript
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>

const socket = new SockJS("http://localhost:8080/ws-stomp");
const stompClient = Stomp.over(socket);
// 👉 해당 설정을 해줘야 sub, pub 시 로그기 안나옴
stomp.debug = null;

// 편의상 Get방식을 사용함
const urlParams = new URL(location.href).searchParams;
const roomName  = urlParams.get('roomName');
const roomId    = urlParams.get('roomId');
const username  = "8080포트";

// 🎶 Connect 
stompClient.connect({}, () => {

   // 👍 구독
   stompClient.subscribe("/sub/chat/room/" + roomId, function (chat) {
       const content = JSON.parse(chat.body);
       const writer = content.writer;
       const str = `<div class='col-6'>
                    <div class='alert ${writer === username ?"alert-secondary" :  "alert-warning"}'>
                      <b> ${writer} : ${content.message} </b>
                      </div>
                  </div>`;
       document.querySelector("#msgArea").insertAdjacentHTML("beforeEnd",str);
   });

   // 🤩 메세지 전송 - 최초 1회 실행 시킴 ( 입장 글 전송 )
   stompClient.send('/pub/chat/enter', {}, JSON.stringify({roomId: roomId, writer: username}))
});

// 😊 메세지 전송 버튼 이벤트
document.querySelector("#button-send").addEventListener("click", (e)=> {
    const msg = document.getElementById("msg");
    // 👉 메세지 전송
    stompClient.send('/pub/chat/message', {}, JSON.stringify({roomId: roomId, message: msg.value, writer: username}));
    msg.value = '';
});
```



