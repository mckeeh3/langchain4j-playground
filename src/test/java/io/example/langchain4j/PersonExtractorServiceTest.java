package io.example.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;

class PersonExtractorServiceTest {
  static final Logger log = LoggerFactory.getLogger(PersonExtractorServiceTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  record Person(String firstName, String lastName, LocalDate birthDate) {
  }

  interface PersonExtractorService {
    Person extractPerson(String text);
  }

  @Test
  void extractPersonFromLongTextTest() {
    var text = """
        In 1968. amidst the fading echoes of Independence Day,
        a child named John arrived under the calm evening sky.
        This newborn bearing the surname Doe, marked the start
        of a New Journey
          """;

    var service = AiServices.create(PersonExtractorService.class, model);
    var person = service.extractPerson(text);

    assertEquals("John", person.firstName());
    assertEquals("Doe", person.lastName());
    assertEquals(LocalDate.of(1968, 7, 5), person.birthDate());

    log.info("person: {}", person);
  }
}
