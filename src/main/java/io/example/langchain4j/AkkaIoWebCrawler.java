package io.example.langchain4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AkkaIoWebCrawler {
  static final Logger log = LoggerFactory.getLogger(AkkaIoWebCrawler.class);
  final Set<String> crawledPages = new HashSet<String>();

  long startTime = System.nanoTime();

  public static void main(String[] args) {
    var url = "https://akka.io";

    var crawler = new AkkaIoWebCrawler();
    crawler.crawlPage(url);
  }

  void crawlPage(String url) {
    try {
      var start = System.nanoTime();
      var webPage = Jsoup.connect(url).get();
      System.out.println("%s\n%s\n\n".formatted(url, webPage.title()));

      crawledPages.add(url);
      System.out.println("Crawled %d pages".formatted(crawledPages.size()));

      var elapsed = System.nanoTime() - start;
      System.out.println("Cycle time: %s".formatted(formatNanoTime(elapsed)));
      System.out.println("Elapsed time: %s".formatted(formatNanoTime(System.nanoTime() - startTime)));

      getPageLinks(webPage).stream()
          .filter(link -> !crawledPages.contains(link))
          .toList()
          .forEach(link -> crawlPage(link));
    } catch (IOException e) {
      System.err.println("Unable to access: %s, %s".formatted(url, e.getMessage()));
    }
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
