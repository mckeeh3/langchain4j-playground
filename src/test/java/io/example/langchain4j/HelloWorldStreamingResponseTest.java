package io.example.langchain4j;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

class HelloWorldStreamingResponseTest {
  static final Logger log = LoggerFactory.getLogger(HelloWorldStreamingResponseTest.class);

  final OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void testHelloWorldStreaming() {
    var service = AiServices.create(ChatService.class, model);
    var message = "Please describe the typical weather in Vilnius in the winter.";

    service.chat(message)
        .onNext(t -> log.info("token: {}", t))
        .onComplete(c -> log.info("Completed: {}", c))
        .onError(e -> log.error("Error: {}", e))
        .start();
  }

  interface ChatService {
    TokenStream chat(String message);
  }
}
