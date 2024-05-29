package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

class ToolsTextMathTest {
  static final Logger log = LoggerFactory.getLogger(ToolsTextMathTest.class);

  final ChatLanguageModel model = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  @Test
  void calculatorToolTest() {
    var memory = MessageWindowChatMemory.withMaxMessages(10);
    var service = AiServices.builder(ChatService.class)
        .chatLanguageModel(model)
        .chatMemory(memory)
        .tools(new Calculator())
        .build();
    var word1 = "Hello";
    var word2 = "Vilnius";
    var sqrt = "%1.2f".formatted(Math.sqrt(word1.length() + word2.length()));
    var question = """
        What is the square root of the sum of the letters
        in the words \"%s\" and \"%s\"?
        """.formatted(word1, word2);
    var answer = service.chat(question);

    log.info("Answer: {}", answer);
    assertTrue(answer.contains(sqrt));
  }

  interface ChatService {
    String chat(String message);
  }

  class Calculator {
    @Tool("Calculate the length of the string")
    public int length(String s) {
      System.out.println("Calculating the length of \"%s\", length %d".formatted(s, s.length()));
      return s.length();
    }

    @Tool("Calculate the sum of two numbers")
    public int sum(int a, int b) {
      System.out.println("Calculating the sum of %d and %d, sum %d".formatted(a, b, a + b));
      return a + b;
    }

    @Tool("Calculate the square root of a number")
    public double sqrt(double a) {
      System.out.println("Calculating the square root of %1.2f, sqrt %1.5f".formatted(a, Math.sqrt(a)));
      return Math.sqrt(a);
    }
  }
}
