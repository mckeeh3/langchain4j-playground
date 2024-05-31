package io.example.langchain4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.chromadb.ChromaDBContainer;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class ReadWebPageTest {
  static final Logger log = LoggerFactory.getLogger(ReadWebPageTest.class);

  @Test
  void readWebPageTest() throws IOException {
    var hostname = "akka.io";
    var url = "https://%s".formatted(hostname);
    var webPage = Jsoup.connect(url).get();

    assertNotNull(webPage);

    var links = webPage.select("a[href]");
    var akkaIoLinks = links.stream()
        .map(link -> link.attr("abs:href"))
        .filter(link -> link.contains(hostname))
        .map(link -> link.replace("language=scala", "language=java"))
        .toList();
    assertTrue(akkaIoLinks.size() > 0);

    var document = Document.from(webPage.text());
    document.metadata().put("url", url);
    document.metadata().put("title", webPage.title());
  }

  @Test
  void injestWebPageTest() throws IOException {
    var url = "https://akka.io";
    var webPage = Jsoup.connect(url).get();

    assertNotNull(webPage);

    var links = webPage.select("a[href]");
    var akkaIoLinks = links.stream()
        .map(link -> link.attr("abs:href"))
        .filter(link -> link.contains("akka.io"))
        .map(link -> link.replace("language=scala", "language=java"))
        .toList();
    assertTrue(akkaIoLinks.size() > 0);

    var document = Document.from(webPage.text());
    document.metadata().put("url", url);
    document.metadata().put("title", webPage.title());

    try (var container = new ChromaDBContainer("chromadb/chroma:0.4.22")) {
      container.start();

      var embeddingStore = ChromaEmbeddingStore.builder()
          .baseUrl(container.getEndpoint())
          .collectionName(UUID.randomUUID().toString())
          .build();

      var embeddingModel = new AllMiniLmL6V2EmbeddingModel();

      var ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
          .textSegmentTransformer(textSegment -> TextSegment.from(
              "title: %s\n\n%s".formatted(textSegment.metadata().getString("title"), textSegment.text()),
              textSegment.metadata()))

          .embeddingModel(embeddingModel)
          .embeddingStore(embeddingStore)
          .build();

      ingestor.ingest(document);

      {
        var queryText = "What is the current Akka license?";
        var embeddingMatch = getEmbeddingMatch(embeddingStore, embeddingModel, queryText);

        log.info("score: {}", embeddingMatch.score());
        log.info("text: {}", embeddingMatch.embedded().text());

        assertTrue(embeddingMatch.embedded().text().contains("BSL"));
      }

      {
        var queryText = "What is the current Akka license version?";
        var embeddingMatch = getEmbeddingMatch(embeddingStore, embeddingModel, queryText);

        log.info("score: {}", embeddingMatch.score());
        log.info("text: {}", embeddingMatch.embedded().text());

        assertTrue(embeddingMatch.embedded().text().contains("BSL"));
      }
    }
  }

  private EmbeddingMatch<TextSegment> getEmbeddingMatch(ChromaEmbeddingStore embeddingStore,
      AllMiniLmL6V2EmbeddingModel embeddingModel, String queryText) {
    var queryEmbedding = embeddingModel.embed(queryText).content();
    var query = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(1)
        .build();
    var relevant = embeddingStore.search(query).matches();
    var embeddingMatch = relevant.get(0);
    return embeddingMatch;
  }
}
