package io.example.langchain4j;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class AkkaIoWebCrawler {
  static final Logger log = LoggerFactory.getLogger(AkkaIoWebCrawler.class);
  final Set<String> crawledPages = new HashSet<String>();
  final EmbeddingStoreIngestor ingestor;
  Queue<String> pagesToCrawl = new LinkedList<>();

  public AkkaIoWebCrawler(EmbeddingStoreIngestor ingestor) {
    this.ingestor = ingestor;
  }

  long startTime = System.nanoTime();

  public static void main(String[] args) {
    var url = "https://akka.io";

    var crawler = new AkkaIoWebCrawler(ingestor());
    crawler.crawl(url);
  }

  void crawl(String url) {
    pagesToCrawl.add(url);

    while (!pagesToCrawl.isEmpty()) {
      var page = pagesToCrawl.poll();
      var links = crawlPage(page);
      pagesToCrawl.addAll(links.stream()
          .filter(link -> !crawledPages.contains(link))
          .filter(link -> !pagesToCrawl.contains(link))
          .toList());
    }
  }

  List<String> crawlPage(String url) {
    try {
      if (crawledPages.contains(url)) {
        // log.info("|=====> Already crawled: {}", url);
        return List.of();
      }
      var start = System.nanoTime();
      var webPage = Jsoup.connect(url).get();

      ingestor.ingest(toDocument(url, webPage));

      crawledPages.add(url);
      log.info("Crawled %,d pages of %,d queued".formatted(crawledPages.size(), pagesToCrawl.size()));

      var elapsed = System.nanoTime() - start;
      var elapsedRun = System.nanoTime() - startTime;

      log.info("Link: {}", url);
      log.info("Title: {}", webPage.title());
      log.info("Cycle time: {}", formatNanoTime(elapsed));
      log.info("Elapsed time: {}", formatNanoTime(elapsedRun));

      return getPageLinks(webPage);
    } catch (IOException e) {
      log.error("Unable to access: {}, {}", url, e.getMessage());
      return List.of();
    }
  }

  dev.langchain4j.data.document.Document toDocument(String url, Document webPage) {
    var document = dev.langchain4j.data.document.Document.from(webPage.text());
    document.metadata().put("url", url);
    document.metadata().put("title", webPage.title());

    return document;
  }

  static EmbeddingStoreIngestor ingestor() {
    var embeddingStore = ChromaEmbeddingStore.builder()
        .baseUrl("http://localhost:8000")
        .collectionName("akka-io-page")
        .build();

    var embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    return EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
        .textSegmentTransformer(textSegment -> TextSegment.from(
            "title: %s\n\n%s".formatted(textSegment.metadata().getString("title"), textSegment.text()),
            textSegment.metadata()))
        .embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel)
        .build();
  }

  List<String> getPageLinks(Document webPage) {
    var links = webPage.select("a[href]");
    return links.stream()
        .map(link -> link.attr("abs:href"))
        .filter(link -> link.contains("akka.io"))
        .map(link -> link.replace("language=scala", "language=java"))
        .map(link -> link.split("#")[0])
        .toList();
  }

  public String formatNanoTime(long nanoSeconds) {
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
