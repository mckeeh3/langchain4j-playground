package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

class SentimentTest {
  static final Logger log = LoggerFactory.getLogger(SentimentTest.class);

  final ChatLanguageModel model0 = OpenAiChatModel.withApiKey(ApiKeys.openAiApiKey);
  final ChatLanguageModel model = OpenAiChatModel.builder() // This is not working with gtp-4o
      .apiKey(ApiKeys.openAiApiKey)
      // .modelName("gpt-4o")
      .modelName("gpt-4")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.2)
      .build();

  @Test
  void sentimentOfPositiveTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.sentimentOf("I love Java.");

    assertEquals(Sentiment.POSITIVE, response);
    log.info("response: {}", response);
  }

  @Test
  void sentimentOfNeutralTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.sentimentOf("Java is ok.");

    assertEquals(Sentiment.NEUTRAL, response);
    log.info("response: {}", response);
  }

  @Test
  void sentimentOfNegativeTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.sentimentOf("I hate Java.");

    assertEquals(Sentiment.NEGATIVE, response);
    log.info("response: {}", response);
  }

  @Test
  void isPositiveWithPositiveTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.isPositive("I really love Java!");

    assertTrue(response);
    log.info("response: {}", response);
  }

  @Test
  void isPositiveWithNeutralTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.isPositive("Java take it or leave it.");

    assertFalse(response);
    log.info("response: {}", response);
  }

  @Test
  void isNeutralWithNeutralTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.isNeutral("Java is okay.");

    assertTrue(response);
    log.info("response: {}", response);
  }

  @Test
  void isPositiveWithNegativeTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.isPositive("I hate Java!");

    assertFalse(response);
    log.info("response: {}", response);
  }

  @Test
  void isNegativeWithNegativeTextTest() {
    var service = AiServices.create(SentimentService.class, model);
    var response = service.isNegative("I hate Java!");

    assertTrue(response);
    log.info("response: {}", response);
  }

  enum Sentiment {
    POSITIVE, NEGATIVE, NEUTRAL
  }

  interface SentimentService {
    @UserMessage("Analyze the sentiment of the following: {{it}}")
    Sentiment sentimentOf(String text);

    @UserMessage("Does the following statement have a positive sentiment? {{it}}")
    boolean isPositive(String text);

    @UserMessage("Does the following statement have a negative sentiment? {{it}}")
    boolean isNegative(String text);

    @UserMessage("Does the following statement have a neutral sentiment? {{text}}")
    boolean isNeutral(@V("text") String text);
  }
}
