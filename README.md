# real-timeStudy

### 서버 -> Client로 이벤트를 보내는 방법
  1. Polling
  2. Long-Polling
  3. SSE(Sever-Sent Event)
  4. Websocket

### Polling
- Client애서 **주기적인 반복(Loop)을** 통해 서버에 요청을 날려 응답을 받아 실시간 처럼 보이는 방법
- 가장 쉬운 방법이지만 **서버의 부하가 높다.**
- `Http OverHead`가 발샐할 확률이 높다.
  - 일반적인 `Http Method`는 header의 정보가 크기에 데이터 처리량이나 처리 시간이 증가하는 것을 말한다.
- **일정한 주기( 짧지 않은 주기 )로** 데이터를 불러와 사용하는 경우에는 나쁘지 않은 방식이다.
       
