package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

class TranslatorServiceTest {
  static final Logger log = LoggerFactory.getLogger(TranslatorServiceTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void testTranslatorService() {
    var service = AiServices.create(TranslatorService.class, model);
    var response = service.translate("Hello, world!", "French");

    assertNotNull(response);
    assertEquals("Bonjour, le monde !", response);
    log.info("response: {}", response);
  }

  interface TranslatorService {
    @SystemMessage("You are a professional translator into {{language}}.")
    @UserMessage("Translate the following: {{text}}.")
    String translate(@V("text") String text, @V("language") String language);
  }
}
