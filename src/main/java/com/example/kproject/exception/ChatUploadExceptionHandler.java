package com.example.kproject.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatUploadExceptionHandler {

    @ExceptionHandler(ChatUploadException.class)
    public ResponseEntity<ProblemDetail> handleChatUploadException(ChatUploadException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid KakaoTalk chat upload");
        return ResponseEntity.badRequest().body(problemDetail);
    }
}
