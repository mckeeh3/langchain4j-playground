# langchain4j-playground
Playing around with LangChain4J

## Crawling Akka.io and loading to vector database

The AkkaIoCrawler.java crawls the Akka.io web site.
Web pages are loaded into a vector database.

Use the following to run the crawler:

~~~bash
mvn exec:java -Dexec.mainClass="io.example.langchain4j.AkkaIoWebCrawler"
~~~

## Chatting with Akka.io

The AkkaIoChat.java utility is a RAG app that takes
prompts, queries the vector database for relevant
web content, then builds an augmented prompt. 
The augmented prompt is then sent to Chat-GPT-4o.

Use the following to run the Akka.io chat:

~~~java
mvn  exec:java -Dexec.mainClass="io.example.langchain4j.AkkaIoChat"
~~~
