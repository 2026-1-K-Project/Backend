package com.example.kproject.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private LocalDateTime createdAt;

    public ChatHistory(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.createdAt = LocalDateTime.now();
    }
}