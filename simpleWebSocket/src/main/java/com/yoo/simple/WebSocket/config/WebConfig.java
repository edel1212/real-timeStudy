package com.yoo.simple.WebSocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * Cors 설정
     * - Spring Security를 사용할 경우 Security내에서 설정해주자!
     *  ㄴ> Spring Security가 활성화된 경우, 기본적으로 HTTP 요청에 대한 보안을 제어하고 CORS 정책도 처리하기 떄문임
     * */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Custom-Header")
                .maxAge(3600);
    }

}
