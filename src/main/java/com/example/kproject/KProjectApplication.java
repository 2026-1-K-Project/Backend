package com.example.kproject;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "K프로젝트 대화 분석 API",
                description = "카카오톡 txt 파일과 채팅 캡처 이미지를 업로드하고, reportId 기반 대화 분석 리포트를 조회하는 API 문서입니다.",
                version = "v1"
        )
)
@SpringBootApplication
public class KProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(KProjectApplication.class, args);
    }

}
