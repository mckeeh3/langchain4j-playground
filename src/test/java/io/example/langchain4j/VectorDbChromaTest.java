package io.example.langchain4j;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.chromadb.ChromaDBContainer;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class VectorDbChromaTest {
  static final Logger log = LoggerFactory.getLogger(VectorDbChromaTest.class);

  @Test
  public void vectorDbChromaTest() {
    try (var container = new ChromaDBContainer("chromadb/chroma:0.4.22")) {
      container.start();

      var embeddingStore = ChromaEmbeddingStore.builder()
          .baseUrl(container.getEndpoint())
          .collectionName(UUID.randomUUID().toString())
          .build();

      var embeddingModel = new AllMiniLmL6V2EmbeddingModel();
      var expectedAnswer = "I like football.";
      {
        var segment = TextSegment.from(expectedAnswer);
        var embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
      }

      {
        var segment = TextSegment.from("Baseball is okay, but it's not my favorite.");
        var embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
      }

      {
        var segment = TextSegment.from("The weather is good today.");
        var embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
      }

      var queryEmbedding = embeddingModel.embed("What is your favorite sport?").content();
      var query = EmbeddingSearchRequest.builder()
          .queryEmbedding(queryEmbedding)
          .maxResults(1)
          .build();
      var relevant = embeddingStore.search(query).matches();
      var embeddingMatch = relevant.get(0);

      log.info("score: {}", embeddingMatch.score());
      log.info("text: {}", embeddingMatch.embedded().text());

      assertEquals(expectedAnswer, embeddingMatch.embedded().text());
    }
  }
}
