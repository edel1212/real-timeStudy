# 실시간 통신 방법

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
- WebSocket(STOMP)
  - Map을 사용해서 메모리에 저장
  - 구현은 간단 하지만 확장성이 떨어진다.
  - [이동](https://github.com/edel1212/real-timeStudy/blob/main/simpleWebSocket)


