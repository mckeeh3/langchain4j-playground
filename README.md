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

**Note:** be sure to run the correct vector database server for the Akka or Kalix web pages.

## Crawling Akka.io and loading to vector database

The AkkaIoCrawler.java crawls the Akka.io web site.
Web pages are loaded into a vector database.

Use the following to run the crawler:

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.AkkaIoWebCrawler"
~~~

This app crawls all of the akka.io web pages. The web page content is embedded in a vector
database.

## Crawling Kalix.io and loading to vector database

The KalixIoCrawler.java crawls the Kalix.io web site.
Web pages are loaded into a vector database.

Use the following to run the crawler:

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.KalixIoWebCrawler"
~~~

This app crawls all of the kalix.io web pages. The web page content is embedded in a vector
database.

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

Provide the following command line argTo run using `Akka` doc type. Default is `akka`.

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.ChatServer" -Dexec.args="akka"
~~~

Provide the following command line argTo run using `Kalix` doc type.

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.ChatServer" -Dexec.args="kalix"
~~~

The server runs on [port 8080](http://localhost:8080).
