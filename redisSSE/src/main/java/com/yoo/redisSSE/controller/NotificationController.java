package com.yoo.redisSSE.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Log4j2
@RequiredArgsConstructor
public class NotificationController {

    public SseEmitter subscribeSSE(){

    }
}
