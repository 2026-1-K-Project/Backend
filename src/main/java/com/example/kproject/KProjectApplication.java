package com.example.kproject;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "K프로젝트 대화 분석 API",
                description = "카카오톡 대화 업로드, 대화 분석 리포트 생성, OpenAI 채팅 테스트를 위한 API 문서입니다.",
                version = "v1"
        )
)
@SpringBootApplication
public class KProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(KProjectApplication.class, args);
    }

}
