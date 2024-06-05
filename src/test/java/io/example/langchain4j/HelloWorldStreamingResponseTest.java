package io.example.langchain4j;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

class HelloWorldStreamingResponseTest {
  static final Logger log = LoggerFactory.getLogger(HelloWorldStreamingResponseTest.class);

  final StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void streamingTest() {
    var message = "Please describe the typical weather in Vilnius in the winter.";

    model.generate(message, new StreamingResponseHandler<AiMessage>() {
      @Override
      public void onNext(String token) {
        System.out.println("onNext: %s".formatted(token));
        log.info("token: {}", token);
      }

      @Override
      public void onComplete(Response<AiMessage> response) {
        System.out.println("onComplete: %s".formatted(response));
        log.info("onComplete: {}", response);
      }

      @Override
      public void onError(Throwable e) {
        System.out.println("onError: %s".formatted(e));
        log.error("onError: {}", e);
      }
    });
  }

  @Test
  void tokenStreamTest() {
    var service = AiServices.create(ChatService.class, model);
    var message = "Please describe the typical weather in Vilnius in the winter.";

    service.chat(message)
        .onNext(t -> {
          System.out.println("token: %s".formatted(t));
          log.info("token: {}", t);
        })
        .onComplete(c -> log.info("Completed: {}", c))
        .onError(e -> log.error("Error: {}", e))
        .start();
  }

  interface ChatService {
    TokenStream chat(String message);
  }
}
