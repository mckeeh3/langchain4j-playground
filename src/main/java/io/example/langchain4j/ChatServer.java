package io.example.langchain4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class ChatServer {
  static final Logger log = LoggerFactory.getLogger(ChatServer.class);
  static final int port = 8080;
  static String docType;

  interface ChatService {
    String chat(String prompt);
  }

  public static void main(String[] args) throws IOException {
    docType = args.length == 0
        ? "akka"
        : args[0].toLowerCase();

    if (!List.of("akka", "kalix").contains(docType)) {
      throw new IllegalArgumentException("Invalid docType: %s".formatted(docType));
    }

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
      var message = requestBody.substring(requestBody.indexOf(":") + 2, requestBody.lastIndexOf("\""));
      return message;
    }

    String generateResponse(String message) {
      var markdownResponse = chat.prompt(message);

      // Convert the Markdown response to HTML
      var parser = Parser.builder().build();
      var document = parser.parse(markdownResponse);
      var renderer = HtmlRenderer.builder().build();
      var htmlResponse = renderer.render(document);

      return enhanceCodeBlocks(htmlResponse);
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
        .collectionName("%s-io-page".formatted(docType))
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
          .maxResults(10)
          .build();
      var relevant = embeddingStore.search(query)
          .matches()
          .stream()
          .filter(match -> match.score() > 0.7)
          .toList();

      log.info("Vector DB response: {}", formatNanoTime(System.nanoTime() - start));
      relevant.forEach(match -> {
        System.out.println("%1.3f, %s".formatted(match.score(), match.embedded().metadata().getString("title")));
      });

      start = System.nanoTime();
      var promptAugmented = new StringBuffer();
      var docTypeCapitalize = docType.substring(0, 1).toUpperCase() + docType.substring(1);
      promptAugmented.append("""
          You are an %s expert.
          Your task is to provide detailed and accurate answers based on the provided %s documentation
          in addition to your %s expertise.\n\n
          Here is some context from the documentation:\n\n""".formatted(docTypeCapitalize, docTypeCapitalize,
          docTypeCapitalize));

      relevant.forEach(match -> promptAugmented
          .append("Title: ")
          .append(match.embedded().metadata().getString("title"))
          .append("\n\n")
          .append(match.embedded().text())
          .append("\n\n"));

      promptAugmented
          .append("The user asked: %s\n\n".formatted(prompt))
          .append("Provide a step-by-step explanation and include code examples if applicable.");

      start = System.nanoTime();
      var response = chatService.chat(promptAugmented.toString());
      log.info("Chat response: {}", formatNanoTime(System.nanoTime() - start));

      log.info("========================================");
      log.info("prompt: {}", prompt);
      relevant.forEach(match -> log.info("Score: {}, Title: {}",
          match.score(),
          match.embedded().metadata().getString("title")));
      log.info("promptAugmented: {}", promptAugmented.toString());
      log.info("========================================");
      log.info("response: {}", response);

      return responseWithReferences(relevant, response);
    }
  }

  static String responseWithReferences(List<EmbeddingMatch<TextSegment>> relevant, String response) {
    var responseWithReferences = new StringBuffer(response);
    responseWithReferences.append("\n\nReferences:\n");
    references(relevant)
        .forEach(
            (url, title) -> responseWithReferences.append("\n[%s](%s)</br>".formatted(title, url)));

    return responseWithReferences.toString();
  }

  static Map<String, String> references(List<EmbeddingMatch<TextSegment>> relevant) {
    var references = new HashMap<String, String>();
    relevant.forEach(match -> {
      var metadata = match.embedded().metadata();
      references.put(metadata.getString("url"), metadata.getString("title"));
    });
    return references;
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

  public static String enhanceCodeBlocks(String html) {
    var doc = Jsoup.parse(html);
    var codeBlocks = doc.select("pre > code[class^=language-]");

    for (Element codeBlock : codeBlocks) {
      String languageClass = codeBlock.className();
      String language = languageClass.replace("language-", "").toUpperCase();

      Element preElement = codeBlock.parent();
      preElement.before(createHeader(language));
      preElement.attr("style", "position: relative;");
    }

    return doc.body().html();
  }

  private static String createHeader(String language) {
    return "<div class='code-header'>"
        + "<span>" + language + "</span>"
        + "<button class='copy-button' onclick=\"copyCode(this)\">Copy</button>"
        + "</div>";
  }
}
