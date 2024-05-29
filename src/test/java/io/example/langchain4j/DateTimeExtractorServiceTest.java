package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

class DateTimeExtractorServiceTest {
  static final Logger log = LoggerFactory.getLogger(DateTimeExtractorServiceTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();
  final String text = """
      It's 27 minutes before midnight on the eve of Christmas 11 years after the birth of
      a young boy born in 2022.
      """;

  @Test
  void extractDateTest() {
    var service = AiServices.create(DateTimeExtractorService.class, model);
    var response = service.extractDate(text);

    assertEquals(LocalDate.of(2033, 12, 24), response);
    log.info("response: {}", response);
  }

  @Test
  void extractTimeTest() {
    var service = AiServices.create(DateTimeExtractorService.class, model);
    var response = service.extractTime(text);

    assertEquals(LocalTime.of(23, 33), response);
    log.info("response: {}", response);
  }

  @Test
  void extractDateTimeTest() {
    var service = AiServices.create(DateTimeExtractorService.class, model);
    var response = service.extractDateTime(text);

    assertEquals(LocalDateTime.of(2033, 12, 24, 23, 33), response);
    log.info("response: {}", response);
  }

  interface DateTimeExtractorService {
    @UserMessage("Extract the date from the following: {{it}}.")
    LocalDate extractDate(String text);

    @UserMessage("Extract the time from the following: {{it}}.")
    LocalTime extractTime(String text);

    @UserMessage("Extract the date and time from the following: {{it}}.")
    LocalDateTime extractDateTime(String text);
  }
}
