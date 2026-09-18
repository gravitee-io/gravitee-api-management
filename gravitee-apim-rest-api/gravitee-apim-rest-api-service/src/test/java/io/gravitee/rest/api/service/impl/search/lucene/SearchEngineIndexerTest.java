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
package io.gravitee.rest.api.service.impl.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import java.io.IOException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchEngineIndexerTest {

    private static final String DOCUMENT_ID = "my-api-id";
    private static final String DOCUMENT_TYPE = "api";

    @Mock
    private IndexWriter writer;

    private SearchEngineIndexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new SearchEngineIndexer(writer);
    }

    private Document aDocument() {
        return aDocument(DOCUMENT_ID, DOCUMENT_TYPE);
    }

    private Document aDocument(String id, String type) {
        Document document = new Document();
        document.add(new StringField("id", id, Field.Store.YES));
        document.add(new StringField("type", type, Field.Store.YES));
        return document;
    }

    @Nested
    class WhenTheWriterSucceeds {

        private ByteBuffersDirectory indexDirectory;
        private IndexWriter indexWriter;
        private SearchEngineIndexer indexerOverARealIndex;

        @BeforeEach
        void openARealIndex() throws Exception {
            indexDirectory = new ByteBuffersDirectory();
            indexWriter = new IndexWriter(indexDirectory, new IndexWriterConfig(new StandardAnalyzer()));
            indexerOverARealIndex = new SearchEngineIndexer(indexWriter);
        }

        @AfterEach
        void closeTheIndex() throws Exception {
            indexWriter.close();
        }

        @Test
        void should_return_the_sequence_number_and_commit_when_commit_is_requested() throws Exception {
            long firstSequenceNumber = indexerOverARealIndex.index(aDocument(), true);
            long secondSequenceNumber = indexerOverARealIndex.index(aDocument(), true);

            assertThat(firstSequenceNumber).isNotNegative();
            assertThat(secondSequenceNumber).isGreaterThan(firstSequenceNumber);
            try (DirectoryReader committedIndex = DirectoryReader.open(indexDirectory)) {
                assertThat(committedIndex.numDocs()).isEqualTo(1);
                assertThat(countMatching(committedIndex, "id", DOCUMENT_ID)).isEqualTo(1);
            }
        }

        @Test
        void should_not_commit_when_commit_is_not_requested() throws Exception {
            long sequenceNumber = indexerOverARealIndex.index(aDocument(), false);

            assertThat(sequenceNumber).isNotNegative();
            assertThat(DirectoryReader.indexExists(indexDirectory)).isFalse();
        }

        @Test
        void should_delete_the_documents_matching_both_id_and_type() throws Exception {
            indexerOverARealIndex.index(aDocument(), false);
            givenTheIndexAlsoHoldsTheSameIdUnderAnotherTypeAndAnotherIdUnderTheSameType();
            indexerOverARealIndex.commit();

            indexerOverARealIndex.remove(aDocument());
            indexerOverARealIndex.commit();

            try (DirectoryReader committedIndex = DirectoryReader.open(indexDirectory)) {
                assertThat(committedIndex.numDocs()).isEqualTo(2);
                assertThat(countMatching(committedIndex, "id", DOCUMENT_ID)).isEqualTo(1);
                assertThat(countMatching(committedIndex, "type", "page")).isEqualTo(1);
                assertThat(countMatching(committedIndex, "id", "another-api-id")).isEqualTo(1);
            }
        }

        @Test
        void should_commit_the_index() throws Exception {
            indexerOverARealIndex.index(aDocument(), false);
            assertThat(DirectoryReader.indexExists(indexDirectory)).isFalse();

            indexerOverARealIndex.commit();

            assertThat(DirectoryReader.indexExists(indexDirectory)).isTrue();
            try (DirectoryReader committedIndex = DirectoryReader.open(indexDirectory)) {
                assertThat(committedIndex.numDocs()).isEqualTo(1);
                assertThat(countMatching(committedIndex, "id", DOCUMENT_ID)).isEqualTo(1);
            }
        }

        private void givenTheIndexAlsoHoldsTheSameIdUnderAnotherTypeAndAnotherIdUnderTheSameType() throws IOException {
            indexWriter.addDocument(aDocument(DOCUMENT_ID, "page"));
            indexWriter.addDocument(aDocument("another-api-id", DOCUMENT_TYPE));
        }

        private int countMatching(DirectoryReader reader, String field, String value) throws IOException {
            return new IndexSearcher(reader).count(new TermQuery(new Term(field, value)));
        }
    }

    @Nested
    class WhenTheWriterFails {

        private final IOException writerFailure = new IOException("disk is full");

        @Test
        void should_wrap_an_index_failure_into_a_technical_exception() throws Exception {
            when(writer.updateDocument(any(Term.class), any())).thenThrow(writerFailure);

            assertThatThrownBy(() -> indexer.index(aDocument(), true))
                .isInstanceOf(TechnicalException.class)
                .hasCauseReference(writerFailure);
        }

        @Test
        void should_wrap_a_remove_failure_into_a_technical_exception() throws Exception {
            when(writer.deleteDocuments(any(Query.class))).thenThrow(writerFailure);

            assertThatThrownBy(() -> indexer.remove(aDocument()))
                .isInstanceOf(TechnicalException.class)
                .hasCauseReference(writerFailure);
        }

        @Test
        void should_wrap_a_commit_failure_into_a_technical_exception() throws Exception {
            when(writer.commit()).thenThrow(writerFailure);

            assertThatThrownBy(() -> indexer.commit())
                .isInstanceOf(TechnicalException.class)
                .hasCauseReference(writerFailure);
        }
    }
}
