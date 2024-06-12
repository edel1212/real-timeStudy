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
    - 
