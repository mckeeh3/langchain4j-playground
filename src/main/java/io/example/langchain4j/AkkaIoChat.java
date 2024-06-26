package io.example.langchain4j;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class AkkaIoChat {
  static final Logger log = LoggerFactory.getLogger(AkkaIoChat.class);

  final ChromaEmbeddingStore embeddingStore = ChromaEmbeddingStore.builder()
      .baseUrl("http://localhost:8000")
      .collectionName("akka-io-page")
      .build();

  final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

  final ChatLanguageModel modelChat = OpenAiChatModel.builder()
      .apiKey(ApiKeys.openAiApiKey)
      .modelName("gpt-4o")
      .logRequests(true)
      .logResponses(true)
      .temperature(0.5)
      .build();

  final ChatService chatService = AiServices.create(ChatService.class, modelChat);

  public static void main(String[] args) {
    var akkaIoChat = new AkkaIoChat();
    var scanner = new Scanner(System.in);
    var prompt = "";

    System.out.println("Enter /exit to quit");

    while (!prompt.equalsIgnoreCase("/exit")) {
      System.out.print("> ");
      prompt = scanner.nextLine();

      if (!prompt.isBlank() && !prompt.equalsIgnoreCase("/exit")) {
        akkaIoChat.chat(prompt);
      }
    }
    scanner.close();
  }

  void chat(String prompt) {
    var start = System.nanoTime();
    var queryEmbedding = embeddingModel.embed(prompt).content();
    var query = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(5)
        .build();
    var relevant = embeddingStore.search(query).matches();
    System.out.println("\nVector DB response: %s".formatted(formatNanoTime(System.nanoTime() - start)));
    relevant.forEach(match -> {
      System.out.println("%1.3f, %s".formatted(match.score(), match.embedded().metadata().getString("title")));
    });

    start = System.nanoTime();
    var promptAugmented = new StringBuffer();
    promptAugmented.append("""
        Please review the following reference material to help
        you to answer the prompt at the end of this request:\n\n""");
    relevant.forEach(match -> promptAugmented
        .append("Title: ")
        .append(match.embedded().metadata().getString("title"))
        .append("\n\n")
        .append(match.embedded().text())
        .append("\n\n"));
    promptAugmented.append(prompt);

    start = System.nanoTime();
    var response = chatService.chat(promptAugmented.toString());
    System.out.println("\nChat response: %s\n".formatted(formatNanoTime(System.nanoTime() - start)));

    System.out.println("%s".formatted(response));

    log.info("========================================");
    log.info("prompt: {}", prompt);
    relevant.forEach(match -> log.info("Score: {}, Title: {}",
        match.score(),
        match.embedded().metadata().getString("title")));
    log.info("promptAugmented: {}", promptAugmented.toString());
    log.info("========================================");
    log.info("response: {}", response);
  }

  interface ChatService {
    String chat(String prompt);
  }

  static String formatNanoTime(long nanoSeconds) {
    long hours = TimeUnit.NANOSECONDS.toHours(nanoSeconds);
    nanoSeconds -= TimeUnit.HOURS.toNanos(hours);
    long minutes = TimeUnit.NANOSECONDS.toMinutes(nanoSeconds);
    nanoSeconds -= TimeUnit.MINUTES.toNanos(minutes);
    long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoSeconds);
    nanoSeconds -= TimeUnit.SECONDS.toNanos(seconds);
    long milliseconds = TimeUnit.NANOSECONDS.toMillis(nanoSeconds);

    return String.format("%d:%02d:%02d:%03dms", hours, minutes, seconds, milliseconds);
  }
}
