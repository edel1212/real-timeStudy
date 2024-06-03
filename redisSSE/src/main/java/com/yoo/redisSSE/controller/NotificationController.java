package com.yoo.redisSSE.controller;

import com.yoo.redisSSE.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(path = "/api/v1/notifications/stream")
    public SseEmitter streamNotifications(String username) {
        return notificationService.createEmitter(username);
    }
}