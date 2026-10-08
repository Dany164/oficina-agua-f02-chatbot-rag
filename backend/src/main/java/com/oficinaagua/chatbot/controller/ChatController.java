package com.oficinaagua.chatbot.controller;

import com.oficinaagua.chatbot.api.ChatApi;
import com.oficinaagua.chatbot.api.model.ChatRequestDto;
import com.oficinaagua.chatbot.api.model.ChatResponseDto;
import com.oficinaagua.chatbot.api.model.SourceDto;
import com.oficinaagua.chatbot.service.rag.RagAnswer;
import com.oficinaagua.chatbot.service.rag.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController implements ChatApi {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public ResponseEntity<ChatResponseDto> sendMessage(ChatRequestDto chatRequestDto) {
        RagAnswer result = ragService.answer(chatRequestDto.getMessage());
        return ResponseEntity.ok(toResponse(result));
    }

    private ChatResponseDto toResponse(RagAnswer result) {
        ChatResponseDto response = new ChatResponseDto();
        response.setAnswer(result.answer());
        response.setSources(result.sources().stream()
                .map(source -> {
                    SourceDto dto = new SourceDto();
                    dto.setTitle(source.title());
                    dto.setDocumentId(source.documentId());
                    return dto;
                })
                .toList());
        return response;
    }
}