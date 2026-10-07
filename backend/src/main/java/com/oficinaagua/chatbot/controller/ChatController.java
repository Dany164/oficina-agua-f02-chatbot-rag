package com.oficinaagua.chatbot.controller;

import com.oficinaagua.chatbot.api.ChatApi;
import com.oficinaagua.chatbot.api.model.ChatRequestDto;
import com.oficinaagua.chatbot.api.model.ChatResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatController implements ChatApi {

    @Override
    public ResponseEntity<ChatResponseDto> sendMessage(ChatRequestDto chatRequestDto) {
        ChatResponseDto response = new ChatResponseDto();
        response.setAnswer("Respuesta de prueba: el motor RAG aun no esta implementado.");
        response.setSources(List.of());
        return ResponseEntity.ok(response);
    }
}

