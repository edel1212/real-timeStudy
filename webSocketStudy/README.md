# WebSocket Study

## 사용 방법

### Dependencies 추가

#### build.gradle

```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-websocket'
}
```
### Custom WebSocket Handler 
- TextWebSocketHandler 상속 받아 구현
  - Override 구현 메서드
    - `void afterConnectionEstablished()` : **소켓 통신 연결** 시 실행될 메서드
    - `void handleTextMessage()` : 소켓을 통해 **메세지 전송** 시 실행될 메서드
    - `void afterConnectionClosed()` : 소켓 **통신 종료** 시 실행될 메서드
- `@Component`를 사용해 **Bean 등록**
- 참고 
  - `List<WebSocketSession> list` :  소켓에 연결된 세션 저장 용도 
    - 실제 운영 환경 시 Redis를 통한 구축 필요
```java
@Component
@Log4j2
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * WebSocket 세션 목록(list)에 있는 모든 세션에게 메시지를 보내기 위해 사용됩니다.
     *
     * 2개의 클라이언트 연결 시 들어있는 목록
     *  - [StandardWebSocketSession[id=8feb5e84-0399-b91a-e334-3526ed284250, uri=ws://localhost:8080/ws/chat]
     *  - , StandardWebSocketSession[id=e9ba4e10-747b-bdae-7466-e24b738be127, uri=ws://localhost:8080/ws/chat]]
     * */
    private  static List<WebSocketSession> list = new ArrayList<>();

    /** 통신간 메서드 전송시 사용될 메서드 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 연결된 모든 세션에 전달하기 위한 Loop
        for(WebSocketSession sess: list) {
            sess.sendMessage(message);
        }
    }

    /** Client가 접속 시 호출되는 메서드 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결 목록에 추가
        this.list.add(session);
        log.info(session + " 클라이언트 접속");
    }

    /** Client가 접속 해제 시 호출되는 메서드 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 연결 목록에서 제거
        this.list.remove(session);
        log.info(session + " 클라이언트 접속 해제");
    }

  /**
   * 모든 접속 대상자에게 메세지 전달 
   * @param  message the websocket Message
   * **/
  public void sendMessageToAllClient(String message) throws Exception{
    TextMessage textMessage  = new TextMessage(message);
    log.info("------------------------");
    log.info("소켓에 접속중인 세션 목록 :::{}",list);
    log.info("textMessage :::{}",textMessage);
    log.info("------------------------");

    // 모든 세션에 전달하기위한 Loop
    for(WebSocketSession sess: list) {
      sess.sendMessage(textMessage);
    }
  }
}
```

### WebSocketConfig Class
```properties
# ℹ️ 사용 될 WebSocket에 대한 설정 Class
#    - url 지정 및 커스텀된 메서드 class 주입 필요
```
- `@EnableWebSocket`
  - 웹 소켓 사용 선언

```java
@Configuration
@RequiredArgsConstructor
@EnableWebSocket // WebSocket을 활성화
public class WebSocketConfig implements WebSocketConfigurer {

  // WebSocket을 컨트롤하기 위하여 주입
  private final WebSocketHandler chatHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(chatHandler     // 핸들러 주입
                    , "ws/chat")        // 사용될 url 설정
            .setAllowedOrigins("*");    // CORS 설정 모두가 접근 가능
  }
}
```
### 모든 상대에게 Message 전달

```properties
# ℹ️ 관리자의 개입이 필요할 때 사용하는 기능이다. 필수 구현 요소가 아님
```

```java
// Constroller

@Controller
@Log4j2
@RequiredArgsConstructor
public class ChatController {
    private final WebSocketHandler chatHandler;
    @GetMapping("/send")
    @ResponseBody
    public ResponseEntity<String> sendClient(String message) throws Exception{
      chatHandler.sendMsgToAllClient(message);
        return ResponseEntity.ok("success");
    }
}
```

#### Client
- `ws://`로 시작하여 웹 소켓 프로토콜임을 지정
- Path 부분은 WebSocketConfig에서 설정한 동일한 Path 사용 "ws/chat`
- `websocket.onmessage` 부분이 **핵심 기능**

```javascript
// websocket객체 생성
const websocket = new WebSocket("ws://localhost:8080/ws/chat");

// 메세지 전송 버튼
document.querySelector("#button-send").addEventListener("click",()=>{
    send();
})

/**
* Send Message Function
*/
const send = ()=>{
    // text Input
    const msg = document.getElementById("msg");
    // 소켓을 통해 전달
    websocket.send(`${username} : ${msg.value}`);
    // 초기화
    msg.value = '';
}

/**
* Exit WebSocket Function
*/
const onClose = (evt) =>{
    websocket.send(`${username} 님이 방을 나가셨습니다."`);
}

/**
* Join WebSocket Function
*/
const onOpen = (evt) => {
    websocket.send(`${username} 님이 입장하셨습니다."`);
}

/**
* Server <-> Client 상호 작용 Function
* @param msg : Server 전달 받은 데이터
*/
const onMessage = (msg) => {

    // 현재 Client의 아이디를 지정핸다.
    const cur_session = "흑곰";

   // 소켓통신 Server 전달 받은 데이터
    const data = msg.data;
    // Data Split - 상황에 맞게 처리
    const arr = data.split(":");
    // ":"를 기준으로 나눠서 아이디를 가져옴
    const sessionId = arr[0].trim();
    // ":"를 기준으로 나눠서 내용을 가져옴
    const message   = arr[1];

    console.log("sessionID      : " + sessionId);
    console.log("cur_session    : " + cur_session);

    // 내가 쓴글과 상대방이 쓴글을 나눠서 제공 - sessionId == cur_session
    const str = `<div class='col-6'>
                    <div class='alert ${sessionId == cur_session ? "alert-secondary" : "alert-warning'" }'>
                        <b> ${sessionId} :  ${message}</b>
                    </div>
                </div>`
    document.querySelector("#msgArea").insertAdjacentHTML("beforeEnd",str);
}

/**
 *  WebSocket 실행 Method Setting
*/
// 👉 Server <-> Client  상호간 수신된 메시지를 처리하는 역할을 수행 
websocket.onmessage = onMessage;
// WebSocket 연결 Function Matching
websocket.onopen    = onOpen;
// WebSocket 종료 Function Matching
websocket.onclose   = onClose;
```

## SocketJS 

### SocketJS란?
- 브라우저에서 WebSocket을 지원하지 않는 경우에 해결 대안 및 **자동 재접속** 기능 제공
-  Spring에서는 `@EnableWebSocket`을 사용하여 WebSocket을 설정
  - SockJS를 **WebSocket 대체 방식으로 사용**할 수 있음


## SocketJS 주요 특징

1. **WebSocket 대체**: WebSocket을 지원하지 않는 환경에서 WebSocket처럼 동작하여 실시간 통신을 가능하게 함

2. **다양한 폴백 옵션**: WebSocket이 불가능할 경우 자동으로 HTTP 기반의 다른 방식(예: AJAX Long Polling)을 사용하여 통신을 유지합니다.

3. **양방향 통신**: 클라이언트와 서버 간 실시간 양방향 메시징을 지원

4. **호환성**: 다양한 브라우저와 네트워크 환경에서 안정적으로 동작

5. **서버 측 지원**: 서버에서도 SocketJS를 통해 클라이언트와의 실시간 연결을 설정하고 메시지를 주고받을 수 있음
   - `node.js`에서는 `Socket.io`를 사용하는 것이 일반적이다.
   - `Spring`에서는 `SockJS`를 사용하는것이 일반적이다.

### 적용 방법

#### WebSocketConfig Class
- SocketJS 적용 시 보안상 문제로 `setAllowedOrigins("*")` 방식 사용 불가능
  - `setAllowedOriginPatterns()` 방식으로 변경 필요
```java
@Configuration
@RequiredArgsConstructor
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final WebSocketHandler chatHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(chatHandler, "/ws/chat")    // 핸들러 주입 및  사용될 uri 지정           
            //.setAllowedOrigins("*")  ❌ SockJS 사용시 보안상 문제로 "*"사용이 불가능해짐 [CORS 설정 ]
            .setAllowedOriginPatterns("http://localhost:8080", "http://localhost:8081") // ✅ Origin 지정
            .withSockJS();                                                              // ✅ SocketJS 추가
  }
}
```

#### Client
- SocketJS Import 필수
- 기존 WebSocket 객체 생성 방법 변경
  - `new WebSocket` -> `new SocketJS()` 생성 객체 방법 변경
```javascript
<script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.4.0/sockjs.min.js"></script>

/***
 *   ✅ WebSocket가 아닌 SockJS를 사용하여 기동함
 *   
 *   첫번째 인자 : Socket 서버의 URL
 *   두번째 인자 : 일반적인 사용 시에는 null로 설정하면 됩니다. 이 매개변수는 SockJS 클라이언트의 동작에 대한 옵션을 제공할 수 있습니다.
 *   세번째 인자 : SockJS 클라이언트의 전송 방식(transport)을 지정하는 옵션입니다.
*/
const websocket = new SockJS("/ws/chat", null, {transports: ["websocket", "xhr-streaming", "xhr-polling"]});

// 하위 코드는 같음
```

