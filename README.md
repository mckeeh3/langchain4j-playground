# Playing around with LangChain4J

This repo contains various apps and unit tests that explore the LangChain4J library.

The apps focus mainly on loading web pages into a vector database. The database
is then used for exploring how to use vector databases to augment prompts that
are then sent to an AI for generated responses.

The unit tests explore different LangChain4J features.

## Running a vector database locally

See the instructions for installing Chroma database locally.

Once installed, run the database providing a path to a
directory that will contain database files.

~~~bat
chroma run --path <path-to-chroma-db-directory>
~~~

## Crawling Akka.io and loading to vector database

The AkkaIoCrawler.java crawls the Akka.io web site.
Web pages are loaded into a vector database.

Use the following to run the crawler:

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.AkkaIoWebCrawler"
~~~

This app crawls all of the akka.io web pages. The web page content is embedded in a vector
database.

In a recent test, it took about 9 hours to crawl ~192K akka.io web pages. The vector database
grew to about 8.4G.

## Chatting with Akka.io CLI

The AkkaIoChat.java utility is a RAG app that takes
prompts, queries the vector database for relevant
web content, then builds an augmented prompt.
The augmented prompt is then sent to Chat-GPT-4o.

Use the following to run the Akka.io chat:

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.AkkaIoChat"
~~~

## Chatting with Akka.io web UI

As with the above CLI, there is a web UI that you can use to chat with Akka.

Use the following command to run the web server:

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.ChatServer"
~~~

The server runs on [port 8080](http://localhost:8080).
