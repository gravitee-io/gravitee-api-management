/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.catalog.search;

import io.gravitee.rest.api.service.catalog.embedding.EmbeddingService;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import lombok.CustomLog;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.Directory;

/**
 * Indexes catalog items into a dedicated, isolated Lucene directory.
 * Each document stores text fields for BM25 keyword search and a
 * {@link KnnFloatVectorField} for semantic nearest-neighbor search.
 * <p>
 * This indexer is completely independent of the existing {@code SearchEngineIndexer}
 * and writes to its own {@link Directory}.
 */
@CustomLog
public class CatalogSemanticIndexer implements Closeable {

    static final String FIELD_ID = "id";
    static final String FIELD_TITLE = "title";
    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_TYPE = "type";
    static final String FIELD_OWNER = "owner";
    static final String FIELD_TAGS = "tags";
    static final String FIELD_EMBEDDING = "embedding";

    private final IndexWriter indexWriter;
    private final EmbeddingService embeddingService;

    public CatalogSemanticIndexer(Directory directory, EmbeddingService embeddingService) throws IOException {
        this.embeddingService = embeddingService;
        var config = new IndexWriterConfig();
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.indexWriter = new IndexWriter(directory, config);
    }

    /**
     * Index a single catalog item, computing its embedding from title + description.
     */
    public void indexItem(CatalogItem item) throws IOException {
        var doc = buildDocument(item);
        indexWriter.updateDocument(new Term(FIELD_ID, item.getId()), doc);
        log.debug("Indexed catalog item '{}'", item.getId());
    }

    /**
     * Drop the entire index and re-index all provided items.
     */
    public void reindexAll(List<CatalogItem> items) throws IOException {
        indexWriter.deleteAll();
        for (var item : items) {
            indexWriter.addDocument(buildDocument(item));
        }
        indexWriter.commit();
        log.info("Reindexed {} catalog items into semantic index", items.size());
    }

    public void commit() throws IOException {
        indexWriter.commit();
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }

    private Document buildDocument(CatalogItem item) {
        var doc = new Document();

        doc.add(new StringField(FIELD_ID, item.getId(), Field.Store.YES));
        doc.add(new TextField(FIELD_TITLE, item.getTitle(), Field.Store.YES));
        doc.add(new TextField(FIELD_DESCRIPTION, item.getDescription(), Field.Store.YES));

        if (item.getType() != null) {
            doc.add(new StoredField(FIELD_TYPE, item.getType()));
        }
        if (item.getOwner() != null) {
            doc.add(new StoredField(FIELD_OWNER, item.getOwner()));
        }
        if (item.getTags() != null && !item.getTags().isEmpty()) {
            doc.add(new StoredField(FIELD_TAGS, String.join(",", item.getTags())));
        }

        // Embed the combined title + description text.
        // Vector dimensions must match EmbeddingService.dimensions()
        // (768 for Ollama nomic-embed-text, 1536 for OpenAI text-embedding-ada-002).
        String textToEmbed = item.getTitle() + " " + item.getDescription();
        float[] vector = embeddingService.embedText(textToEmbed);
        doc.add(new KnnFloatVectorField(FIELD_EMBEDDING, vector, VectorSimilarityFunction.COSINE));

        return doc;
    }
}
