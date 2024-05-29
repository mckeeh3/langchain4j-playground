package io.example.langchain4j;

import static dev.ai4j.openai4j.image.ImageModel.DALL_E_QUALITY_HD;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class OpenAiImageModelTest {
    static final Logger log = LoggerFactory.getLogger(OpenAiImageModelTest.class);

    final ChatLanguageModel modelChat = OpenAiChatModel.builder()
            .apiKey(ApiKeys.openAiApiKey)
            .modelName("gpt-4o")
            .logRequests(true)
            .logResponses(true)
            .temperature(0.5)
            .build();

    final ImageModel modelImage = OpenAiImageModel.builder()
            .apiKey(ApiKeys.openAiApiKey)
            .modelName("gpt-4o")
            .quality(DALL_E_QUALITY_HD)
            .logRequests(true)
            .logResponses(true)
            .build();

    final ImageModel modelImagePersist = OpenAiImageModel.builder()
            .apiKey(ApiKeys.openAiApiKey)
            .modelName("gpt-4o")
            .quality(DALL_E_QUALITY_HD)
            .logRequests(true)
            .logResponses(true)
            .withPersisting()
            .build();

    @Test
    void imageModelTest() {
        var image = modelImage.generate("A cat in a hat");

        assertTrue(image.content().url().getHost().contains("oaidalle"));

        log.info("Image: {}", image);
        log.info("Image URL: {}", image.content().url());
    }

    @Test
    void imageModelPersistTest() throws URISyntaxException {
        var embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        var embeddingStore = new InMemoryEmbeddingStore<TextSegment>();

        var ingestor = EmbeddingStoreIngestor
                .builder()
                .documentSplitter(DocumentSplitters.recursive(1000, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        var document = loadDocument(
                Paths.get(
                        Objects
                                .requireNonNull(
                                        OpenAiImageModelTest.class.getResource("/story-about-happy-carrot.txt"))
                                .toURI()),
                new TextDocumentParser());
        ingestor.ingest(document);

        var contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();
        var chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(modelChat)
                .contentRetriever(contentRetriever)
                .build();

        var drawPromptTemplate = PromptTemplate.from(
                "Draw {{object}}. Base the picture on following information:\n\n{{information}}");

        var variables = Map.ofEntries(
                Map.entry("information", (Object) chain.execute("Who is Charlie?")),
                Map.entry("object", "Ultra realistic Charlie on the party, cinematic lighting"));

        var response = modelImagePersist.generate(drawPromptTemplate.apply(variables).text());

        log.info("Image: {}", response);
        log.info("Image URL: {}", response.content().url());
    }
}
