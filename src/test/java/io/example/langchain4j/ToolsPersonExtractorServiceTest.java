package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public class ToolsPersonExtractorServiceTest {
  static final Logger log = LoggerFactory.getLogger(ToolsPersonExtractorServiceTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void personExtractorToolTest() {
    var memory = MessageWindowChatMemory.withMaxMessages(10);
    var service = AiServices.builder(PersonExtractorService.class)
        .chatLanguageModel(model)
        .chatMemory(memory)
        .build();
    var text = """
        In 1968. amidst the fading echoes of Independence Day,
        a child named John arrived under the calm evening sky.
        This newborn bearing the surname Doe, marked the start
        of a New Journey.
          """;
    var person = service.chat(text);

    log.info("Person: {}", person);
    assertEquals("John", person.firstName());
    assertEquals("Doe", person.lastName());
    assertEquals(LocalDate.of(1968, 7, 4), person.birthDate());
  }

  @Test
  void picardExtractorToolTest() {
    var memory = MessageWindowChatMemory.withMaxMessages(10);
    var service = AiServices.builder(PersonExtractorService.class)
        .chatLanguageModel(model)
        .chatMemory(memory)
        .tools(new DateTool())
        .build();
    var years = 2305 - LocalDate.now().getYear();
    var text = """
        Jean-Luc Picard is a Starfleet officer and captain of the USS Enterprise.
        He is known for his diplomatic skills, leadership, and commitment to the principles of the Federation.
        Picard is a skilled negotiator and strategist, and he has a deep respect for other cultures and traditions.
        He was born %d years from now on July 13, in La Barre, France.
          """.formatted(years);
    var person = service.chat(text);

    log.info("Person: {}", person);
    assertEquals("Jean-Luc", person.firstName());
    assertEquals("Picard", person.lastName());
    assertEquals(LocalDate.of(2305, 7, 13), person.birthDate());
  }

  interface PersonExtractorService {
    @SystemMessage("""
        Extract the fields in the Person record from the text.
        """)
    @UserMessage("Extract the person information from: {{it}}.")
    Person chat(String message);
  }

  record Person(String firstName, String lastName, LocalDate birthDate) {
  }

  class DateTool {
    static final Logger log = LoggerFactory.getLogger(DateTool.class);

    @Tool("Get the year from the years from now.")
    int yearsFromNow(int years) {
      log.info("Years from now: now + {} = {}", years, LocalDate.now().plusYears(years).getYear());
      return LocalDate.now().plusYears(years).getYear();
    }
  }
}
