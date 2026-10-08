package com.oficinaagua.chatbot.service.rag;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/** Implementación con Spring AI: usa el ChatModel configurado (hoy Ollama). */
@Component
public class SpringAiAnswerGenerator implements AnswerGenerator {

    private final ChatClient chatClient;

    public SpringAiAnswerGenerator(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    @Override
    public String generate(RagPrompt prompt) {
     // Se envían mensajes ya armados (no plantillas): las llaves {} del texto no se interpretan.
        Prompt springPrompt = new Prompt(List.of(
                new SystemMessage(prompt.systemMessage()),
                new UserMessage(prompt.userMessage())));
                return chatClient.prompt(springPrompt).call().content();

    }
}
