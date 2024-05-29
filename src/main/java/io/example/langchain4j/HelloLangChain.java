package io.example.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class HelloLangChain {
  public static void main(String[] args) {
    var model = OpenAiChatModel.builder()
        .apiKey(ApiKeys.openAiApiKey)
        .modelName("gpt-4o")
        .logRequests(true)
        .logResponses(true)
        .temperature(1.0)
        .build();
    var response = model.generate("What is your model name?");

    System.out.println(response);
  }
}