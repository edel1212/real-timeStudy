# 실시간 통신 방법

## WebSocket이란?

- **양방향 통신**을 제공하기 위해 개발된 프로토콜
  - **초기 연결은 HTTP** 요청을 통해 이루어지지만, **연결이 성립된 후**에는 HTTP가 아닌 **WebSocket 프로토콜로 전환**
    - 초기 연결은 HTTP Request를 그대로 사용하기 떄문에 CORS 적용이나 JWT 인증등을 기존과 동일하게 적용이 가능
- 접속까지는 HTTP 프로토콜을 이용하고 그 이후 통신은 자체적은 WebSocket 프로토콜 통신하게 된다.
- 클라이언트가 요청을 하고 웹서버가 응답한 후 연결을 끊는 방식이 아닌 `Connection`을 그대로 **유지 하고** 클라이언트의 요청 없이도  
상호간의 데이터를 전송할수 있는 프로토콜이다.
- 프로토콜의 요청은 `ws://도메인주소:포트번호`로 이루워 진다.


## 적용 시점
```properties
# ℹ️ 웹소켓은 서비스를 동적으로 만들어 주지만, Ajax, Streaming, Long polling 기술이 더 효과적일 경우도 있다.
#    ㄴ 변경 사항의 빈도가 자주 일어나지 않고, 데이터의 크기가 작은 경우 Ajax, Streaming, Long polling 기술이 더 효과적일 수 있음
```
- 실시간성을 보장
- 변경 사항의 빈도가 잦거나 또는 짧은 대기 시간
  - 협업, 게임, 금융, 주식, 코인

## WebSocket과 Socket 비교

![image](https://github.com/user-attachments/assets/619476cc-f09e-483b-9ac2-7911b0313f40)


### 정의
- **WebSocket**
  - 하나의 TCP 연결에서 전이중 통신(양방향 동시 송수신)을 제공하는 컴퓨터 통신 프로토콜이다
  - 초기 연결(핸드셰이크)은 HTTP(S)를 통해 설정하며, 연결이 수립된 이후에는 TCP 연결로 전환하여 통신이 이루어짐
  - 주로 실시간 웹 애플리케이션에서 사용

- **Socket**
  - 네트워크 상에서 프로그램 간 통신의 종착점(Endpoint)을 지칭함
  - TCP 소켓은 연결 지향 방식(1:1 통신)으로 동작하며, UDP 소켓은 비연결 지향 방식으로 데이터그램(패킷) 기반의 통신을 수행
  - 대부분의 네트워크 소켓은 인터넷 소켓(TCP/UDP 기반)

---

### 차이점

| **구분**       | **WebSocket**                                                                            | **Socket**                                                     |
|----------------|-----------------------------------------------------------------------------------------|----------------------------------------------------------------|
| **동작 계층**   | TCP 위에서 동작하며, OSI **5계층(세션 계층)** 또는 **7계층(응용 계층)**에 위치          | TCP/UDP와 같은 전송 계층 프로토콜 위에서 동작하며, **OSI 4계층(전송 계층)**에 속함        |
| **데이터 형식** | 메시지 형식으로 데이터를 주고받으며, 텍스트 또는 바이너리 형식을 지원합니다.                     | TCP 소켓은 바이트 스트림 데이터를 송수신하며, UDP 소켓은 데이터그램(패킷) 단위로 데이터를 송수신합니다. |
| **주요 목적**   | 웹 환경에서의 실시간 데이터 교환을 위해 설계되었습니다.                                    | 네트워크 프로그래밍에서 포괄적으로 사용되며, 다양한 프로토콜과 통신 구조에 적용됩니다.               |

---

### 결론

- **WebSocket과 Socket의 관계**  
  WebSocket은 TCP 소켓을 기반으로 설계된 통신 프로토콜로, 웹 환경에서 실시간 양방향 통신을 위해 최적화된 표준입니다.  
  두 개념은 상반되지 않으며, WebSocket은 소켓 통신을 웹 환경에 맞춰 발전시킨 형태이다

### 서버 -> Client로 이벤트를 보내는 방법
  1. Polling
  2. Long-Polling
  3. SSE(Sever-Sent Event)
  4. Websocket

### 1. Polling
- Client애서 **주기적인 반복(Loop)을** 통해 서버에 요청을 날려 응답을 받아 실시간 처럼 보이는 방법
- 가장 쉬운 방법이지만 **서버의 부하가 높다.**
- `Http OverHead`가 발샐할 확률이 높다.
  - 일반적인 `Http Method`는 header의 정보가 크기에 데이터 처리량이나 처리 시간이 증가하는 것을 말한다.
- **일정한 주기( 짧지 않은 주기 )로** 데이터를 불러와 사용하는 경우에는 나쁘지 않은 방식이다.

### 2. Long-Polling
- Client에서 요청이 들어오면 접속을 열어 두어서 원하는 응답이 있을 경우 응답하는 형식
- Polling 방식보다는 서버의 부담은 적지만 이벤트들의 시간 간격이 좋다면 별차이가 없다.
- Polling방식에 비해 크게 이점이 없기에 잘 사용하지 않는다.

### 3. SSE(Sever-Sent Event)
- HTML5 표준안이며 웹소켓과 비슷한 역할을 하면서 더 가볍다
  - `Http Method`에 비해 Header의 내용이 적어 훨씬 가볍다
  - `Http Protocole`을 사용한다.
- 양방향이 아닌 단방향이다.
  - Client -> 사용 요청 -> Server -> 수락 -> Client  이후 Server에서 전달하는 Event 및 Message를 받을 수 있다.
- 재접속 처리 같은 대부분의 저수준 처리가 자동으로 지원된다.
  - 3초 마다 재시도 함
- IE에서 지원하지 않으나 `polyfi`를 사용하거나 다양한 라이브러리로 대처가 가능하다.
- Webscoket만으로도 같은 기능을 하도록 구현이 가능하나 알림 처리에는 SSE가 조금 더 효율적이다
  - 서로 주고받는 형식이 아니기 떄문이다.

#### 구현 예제
- SSE
  - Map을 사용하여 `SseEmitter`을 저장하여 구현
    - 구현은 간단 하지만 확장성이 떨어진다.
    - [이동](https://github.com/edel1212/real-timeStudy/tree/main/simpleSSE)
  - Redis를 사용하여 `SseEmitter`을 저장하여 구현
    - 구현은 복잡 하지만 확장성이 높아진다.
    - [이동](https://github.com/edel1212/real-timeStudy/tree/main/redisSSE)
- WebSocket
  - Only SocketJs
    > STOMP를 메인으로 하기에 Redis 관련 구현 X
    - [이동](https://github.com/edel1212/real-timeStudy/blob/main/webSocketStudy)
  - STOMP
    - Map을 사용해서 메모리에 저장
      - 구현은 간단 하지만 확장성이 떨어진다.
      - [이동](https://github.com/edel1212/real-timeStudy/blob/main/simpleWebSocket)
    - Redis를 사용하여 구현
      - 구현은 복잡 하지만 확장성이 높아진다.
        - `sub / pub` 개념을 사용하여 scale-out이 가능하나 Redis의 특징으로 pub 시 데이터가 유실된다.
          - 읽기 않은 메세지 처리와 같은 기능이 불가능함
          - 모든 구독자에게 메세지는 전송되나 구독을 하지 않으면 해당 정보를 받을 수 없음
          -  이해를 돕기 위한 쉬운 예로 Tv 프로그램을 생각하면된다
            - Tv는 바로바로 구독자들에세 영상을 송출함   
      - [이동](https://github.com/edel1212/real-timeStudy/blob/main/redisWebSocket)


