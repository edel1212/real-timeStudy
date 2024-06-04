package com.yoo.redisSSE.controller;

import com.yoo.redisSSE.service.NotificationService;
import com.yoo.redisSSE.service.NotificationServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    //private final NotificationService notificationService;
    private final NotificationServiceImpl notificationService;

    @GetMapping(value = "/sub", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request, String accountId){
        // ℹ️ 해더 값에 Token을 넣어서 왔을 경우도 수정이 가능하다.
        String authorization =  request.getHeader("Authorization");
        if(authorization != null){ } // TODO Jwt 로직 수행
        return notificationService.subscribe(accountId);
    }

    @PostMapping(value = "/send-data", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void sendData(String accountId, String message) {
        notificationService.sendNotification(accountId, message);
    }
}