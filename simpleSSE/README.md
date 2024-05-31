# SSE(Server Sent Event) - 메모리 사용

- 확장성이 낮으나 구현하기 쉬운 방법 적용
- Client측에서 Header값 변경이 어렵다고하나 다양한 라이브러리를 사용하면 가능하다.
- 메모리를 사용하므로 Map을 활용하나 thread-safe를 위해 `new ConcurrentHashMap()`사용해서 데이터를 저장

### 구독 신청 흐름 코드

- 구독 신청 Controller

  ```java
  @RestController
  @RequiredArgsConstructor
  public class NotificationController {

      private final NotificationService notificationService;

      @GetMapping(value = "/sub", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
      public SseEmitter subscribe(HttpServletRequest request, String accountId){
          // ℹ️ 해더 값에 Token을 넣어서 왔을 경우도 수정이 가능하다.
          String authorization =  request.getHeader("Authorization");
          if(authorization != null){ } // TODO Jwt 로직 수행
          return notificationService.subscribe(accountId);
      }

  }
  ```

- 구독 신청 Service

```java
public interface NotificationService {
    // 타임 아웃 시간
    Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    /**
     * <h3>클라이언트가 구독을 위해 호출하는 메서드.</h3>
     *
     * @param accountId - 구독하는 클라이언트의 사용자 아이디.
     * @return SseEmitter - 서버에서 보낸 이벤트 Emitter
     */
    SseEmitter subscribe(String accountId);

}


@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final EmitterRepository emitterRepository;

    @Override
    public SseEmitter subscribe(String accountId) {
        SseEmitter emitter = createEmitter(accountId);
        log.info("--------------------------");
        log.info("EventStream Created. [userId=" + accountId + "]");
        log.info("--------------------------");
        return emitter;
    }

    /**
     * 사용자 아이디를 기반으로 이벤트 Emitter를 생성
     *
     * @param accountId - 사용자 아이디.
     * @return SseEmitter - 생성된 이벤트 Emitter.
     */
    private SseEmitter createEmitter(String accountId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(accountId, emitter);
        /**
         * ℹ️ 클라이언트가 SSE (Server-Sent Events) 스트림을 정상적으로 종료했을 때 발생합니다.
         *    이는 모든 데이터가 성공적으로 전송되고 더 이상 전송할 데이터가 없을 때 발생합니다.
         * */
        emitter.onCompletion(() -> emitterRepository.deleteById(accountId));
        /**
         * ℹ️ 설정된 시간 동안 이벤트를 전송하지 않았을 때 발생합니다.
         *     DEFAULT_TIMEOUT으로 설정된 시간이 지나면 타임아웃이 발생합니다.
         * */
        emitter.onTimeout(() -> emitterRepository.deleteById(accountId));
        return emitter;
    }

}
```

- 구독 신청 Repository

```java
@Repository
@RequiredArgsConstructor
public class EmitterRepository {
    /**
     * 모든 Emitters를 저장하는 ConcurrentHashMap
     * - 추후 확정성을 위해 Reids로 변경하자
     * -  thread-safe를 위해 HashMap이 아닌 ConcurrentHashMap 사용
     * **/
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 주어진 아이디와 이미터를 저장
     *
     * @param accountId    - 사용자 아이디.
     * @param emitter       - 이벤트 Emitter.
     */
    public void save(String accountId, SseEmitter emitter) {
        emitters.put(accountId, emitter);
    }

    /**
     * 주어진 아이디의 Emitter를 제거
     *
     * @param accountId - 사용자 아이디.
     */
    public void deleteById(String accountId) {
        emitters.remove(accountId);
    }

}
```

### 서버측 메세지 보내기 흐름 코드

- 메세지 보내기 Controller
  - 다양한 방법으로 보낼 수 있다. 스케줄링, 비동기 로직 등등 ..

```java
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping(value = "/send-data", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void sendData(String accountId, String message) {
        notificationService.notify(accountId, message);
    }

}
```

- 메세지 보내기 Service

```java
public interface NotificationService {
    /**
     * 서버의 이벤트를 클라이언트에게 보내는 메서드
     * 다른 서비스 로직에서 이 메서드를 사용해 데이터를 Object event에 넣고 전송하면 된다.
     *
     * @param accountId - 메세지를 전송할 사용자의 아이디.
     * @param event     - 전송할 이벤트 객체.
     */
    void notify(String accountId, Object event);
}

@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final EmitterRepository emitterRepository;

    @Override
    public void notify(String accountId, Object event) {
        this.sendToClient(accountId, event);
    }


    /**
     * 클라이언트에게 데이터를 전송
     *
     * @param accountId   - 데이터를 받을 사용자의 아이디.
     * @param data - 전송할 데이터.
     */
    private void sendToClient(String accountId, Object data) {
        SseEmitter emitter = emitterRepository.get(accountId);
        if (emitter == null) return;
        try {
            SseEmitter.SseEventBuilder  messageEvent
                    = SseEmitter.event()
                    .id(accountId)              // 메세지를 찾을 key
                    .name("sse")     // 이벤트명  : eventSource.addEventListener("sse",()=>{})
                    .data(data);                // 전달 데이터
            emitter.send(messageEvent);
        } catch (IOException exception) {
            emitterRepository.deleteById(accountId);
            emitter.completeWithError(exception);
        } // try - catch
    }

}
```

- 메세지 보내기 Repository

```java
@Repository
@RequiredArgsConstructor
public class EmitterRepository {
    /**
     * 모든 Emitters를 저장하는 ConcurrentHashMap
     * - 추후 확정성을 위해 Reids로 변경하자
     * -  thread-safe를 위해 HashMap이 아닌 ConcurrentHashMap 사용
     * **/
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 주어진 아이디의 Emitter를 가져옴.
     *
     * @param accountId     - 사용자 아이디.
     * @return SseEmitter   - 이벤트 Emitter.
     */
    public SseEmitter get(String accountId) {
        return emitters.get(accountId);
    }
}
```
