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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        Document document = new Document();
        document.add(new StringField("id", DOCUMENT_ID, Field.Store.YES));
        document.add(new StringField("type", DOCUMENT_TYPE, Field.Store.YES));
        return document;
    }

    @Nested
    class WhenTheWriterSucceeds {

        @Test
        void should_return_the_sequence_number_and_commit_when_commit_is_requested() throws Exception {
            when(writer.updateDocument(any(Term.class), any())).thenReturn(42L);

            long sequenceNumber = indexer.index(aDocument(), true);

            assertThat(sequenceNumber).isEqualTo(42L);
            ArgumentCaptor<Term> term = ArgumentCaptor.forClass(Term.class);
            verify(writer).updateDocument(term.capture(), any());
            assertThat(term.getValue()).isEqualTo(new Term("id", DOCUMENT_ID));
            verify(writer).commit();
        }

        @Test
        void should_not_commit_when_commit_is_not_requested() throws Exception {
            when(writer.updateDocument(any(Term.class), any())).thenReturn(7L);

            long sequenceNumber = indexer.index(aDocument(), false);

            assertThat(sequenceNumber).isEqualTo(7L);
            verify(writer, never()).commit();
        }

        @Test
        void should_delete_the_documents_matching_both_id_and_type() throws Exception {
            indexer.remove(aDocument());

            ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
            verify(writer).deleteDocuments(query.capture());
            BooleanQuery expected = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("id", DOCUMENT_ID)), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("type", DOCUMENT_TYPE)), BooleanClause.Occur.MUST)
                .build();
            assertThat(query.getValue()).isEqualTo(expected);
        }

        @Test
        void should_commit_the_index() throws Exception {
            indexer.commit();

            verify(writer).commit();
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
