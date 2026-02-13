package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController // JSON이나 문자열을 바로 웹에 띄워주는 설정
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/") // 로컬호스트 접속 시 실행될 주소
    public String home() {
        return "드디어 로컬 서버 구동 성공! 밥팅 탈출입니다.";
    }
}