package io.example.langchain4j;

import io.github.cdimascio.dotenv.Dotenv;

public class ApiKeys {
  static String openAiApiKey = getEnv("OPENAI_API_KEY");

  private static String getEnv(String key) {
    var dotEnv = Dotenv.load();
    var value = System.getenv().get(key);
    return value == null || value.isBlank()
        ? dotEnv.get(key)
        : value;
  }
}
