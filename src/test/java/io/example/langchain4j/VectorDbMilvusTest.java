package io.example.langchain4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.milvus.MilvusContainer;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

public class VectorDbMilvusTest {
  static final Logger log = LoggerFactory.getLogger(VectorDbMilvusTest.class);

  @Test
  public void vectorDbMilvusTest() {
    try (var container = new MilvusContainer("milvusdb/milvus:v2.3.1")) {
      container.start();
      var embeddingStore = MilvusEmbeddingStore.builder()
          .uri(container.getEndpoint())
          .collectionName("test_collection")
          .dimension(384)
          .build();

      var embeddingModel = new AllMiniLmL6V2EmbeddingModel();
      var expectedAnswer = "I like football.";

      {
        var segment = TextSegment.from(expectedAnswer);
        var embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
      }
      {
        var segment = TextSegment.from("The weather is good today.");
        var embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
      }
      {
        var segment = TextSegment.from("BaseBall is ok, but it's not my favorite.");
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
