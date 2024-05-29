package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

class HelloWorldTest {
  static final Logger log = LoggerFactory.getLogger(HelloWorldTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void testHelloWorld() {
    var response = model.generate("What is your model name?");

    assertTrue(response.contains("GPT-4"));
    log.info("response: {}", response);
  }
}
