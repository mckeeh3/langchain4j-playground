package io.example.langchain4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class ChatServer {
  static final Logger log = LoggerFactory.getLogger(ChatServer.class);
  static final int port = 8080;

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

  interface ChatService {
    String chat(String prompt);
  }

  public static void main(String[] args) throws IOException {
    var server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", new StaticFileHandler());
    server.createContext("/api/chat", new ChatHandler());

    var executor = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(executor);
    server.start();

    log.info("Server started on port {}", port);
  }

  static class ChatHandler implements HttpHandler {
    final Chat chat = new Chat();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        var requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        var message = extractMessage(requestBody);
        var response = generateResponse(message);
        sendResponse(exchange, response);
      } else {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
      }
    }

    String extractMessage(String requestBody) {
      // Extract the "message" field from the JSON request body
      // You can use a JSON parsing library like Gson or Jackson for more robust
      // parsing
      var message = requestBody.substring(requestBody.indexOf(":") + 2, requestBody.lastIndexOf("\""));
      return message;
    }

    String generateResponse(String message) {
      // Process the message and generate a Markdown response
      var markdownResponse = chat.prompt(message);

      // Convert the Markdown response to HTML
      var parser = Parser.builder().build();
      var document = parser.parse(markdownResponse);
      var renderer = HtmlRenderer.builder().build();
      var htmlResponse = renderer.render(document);

      return htmlResponse;
    }

    void sendResponse(HttpExchange exchange, String response) throws IOException {
      var responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, responseBytes.length);
      exchange.getResponseBody().write(responseBytes);
      exchange.close();
    }
  }

  static class StaticFileHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var requestPath = exchange.getRequestURI().getPath();
      if ("/".equals(requestPath)) {
        requestPath = "/index.html";
      }

      var url = ChatServer.class.getResource(requestPath);
      try {
        var filePath = Paths.get(url.toURI());
        if (Files.exists(filePath)) {
          var contentType = getContentType(filePath);
          exchange.getResponseHeaders().set("Content-Type", contentType);
          exchange.sendResponseHeaders(200, 0);
          var outputStream = exchange.getResponseBody();
          Files.copy(filePath, outputStream);
          outputStream.close();
        } else {
          var errorResponse = "404 Not Found";
          exchange.sendResponseHeaders(404, errorResponse.length());
          var outputStream = exchange.getResponseBody();
          outputStream.write(errorResponse.getBytes(StandardCharsets.UTF_8));
          outputStream.close();
        }
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
    }

    String getContentType(Path filePath) {
      var fileName = filePath.getFileName().toString();
      if (fileName.endsWith(".html")) {
        return "text/html";
      } else if (fileName.endsWith(".css")) {
        return "text/css";
      } else if (fileName.endsWith(".js")) {
        return "application/javascript";
      } else {
        return "application/octet-stream";
      }
    }
  }

  static class Chat {
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

    String prompt(String prompt) {
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

      return response;
    }
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
