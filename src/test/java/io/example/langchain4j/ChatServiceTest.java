package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

class ChatServiceTest {
  static final Logger log = LoggerFactory.getLogger(ChatServiceTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void simpleChatServiceTest() {
    var service = AiServices.create(ChatService.class, model);
    var response = service.chat("Hello, world!");

    assertNotNull(response);
    log.info("response: {}", response);
  }

  interface ChatService {
    String chat(String message);
  }
}
