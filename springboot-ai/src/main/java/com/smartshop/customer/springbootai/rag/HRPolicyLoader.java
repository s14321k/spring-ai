package com.smartshop.customer.springbootai.rag;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class HRPolicyLoader {

    Logger logger = LoggerFactory.getLogger(HRPolicyLoader.class);

    private final VectorStore vectorStore;

    @Value("classpath:Eazybytes_HR_Policies.pdf")
    Resource hrPolicyFile;

    public HRPolicyLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadPDF() {
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(hrPolicyFile);
        List<Document> documentList = tikaDocumentReader.get();
        logger.info("Document size (before split): {}", documentList.size());

        // Split large documents into token-sized chunks that fit
        // the embedding model's physical batch size (512 tokens on the
        // local Docker Model Runner instance).
        TokenTextSplitter splitter = TokenTextSplitter.builder()        // Starts building the text splitter
                .withChunkSize(300)                                     // Target ~300 tokens per chunk (stays under model limits)
                .withMinChunkSizeChars(100)                             // Drops chunks smaller than 100 characters
                .withMinChunkLengthToEmbed(5)                           // Ignores chunks shorter than 5 tokens for embedding
                .withMaxNumChunks(400)                                // Hard cap: stops after 10,000 chunks
                .withKeepSeparator(true)                                // Keeps sentence/paragraph separators inside chunks
                .build();                                               // Creates the final splitter instance

//        May use this or the last line approach
//        List<Document> splitDocuments = splitter.apply(documentList);
//        logger.info("Document size (after split): {}", splitDocuments.size());

        vectorStore.add(splitter.split(documentList));
    }
}
