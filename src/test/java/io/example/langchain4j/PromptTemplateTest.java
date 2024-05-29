package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

class PromptTemplateTest {
  static final Logger log = LoggerFactory.getLogger(PromptTemplateTest.class);

  public PromptTemplateTest() {
  }

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void testPromptTemplateHello() {
    var promptTemplate = PromptTemplate
        .from("Translate {{text}} to {{language}}.");
    var prompt = promptTemplate.apply(
        Map.ofEntries(
            Map.entry("text", "Hello, world!"),
            Map.entry("language", "Polish")));

    assertEquals("Translate Hello, world! to Polish.", prompt.text());

    var response = model.generate(prompt.text());

    assertTrue(response.contains("Witaj, świecie!"));

    log.info("response: {}", response);
  }

  @Test
  void testPromptTemplateRecipe() {
    var template = "Create a recipe for {{dish}} with these ingredients {{ingredients}}.";
    var promptTemplate = PromptTemplate.from(template);
    var prompt = promptTemplate.apply(
        Map.ofEntries(
            Map.entry("dish", "chicken curry"),
            Map.entry("ingredients", "chicken, curry powder, rice, water")));

    var response = model.generate(prompt.text());

    assertTrue(response.contains("chicken curry"));

    log.info("response: {}", response);
  }
}
