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
package io.gravitee.rest.api.service.impl.search.lucene.searcher;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.query.Query;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.index.IndexWriter;
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
class AbstractDocumentSearcherTest {

    static class CustomSearcher extends AbstractDocumentSearcher {

        protected CustomSearcher(IndexWriter indexWriter) {
            super(indexWriter);
        }

        @Override
        public SearchResult search(ExecutionContext executionContext, Query<?> query) throws TechnicalException {
            return null;
        }

        @Override
        public boolean handle(Class<? extends Indexable> source) {
            return false;
        }

        public Optional<org.apache.lucene.search.Query> buildFilterQuery(Map<String, Object> filters, Map<String, String> remapFields) {
            return super.buildFilterQuery(filters, remapFields);
        }
    }

    @Mock
    IndexWriter indexWriter;

    CustomSearcher customSearcher;

    @BeforeEach
    void setup() {
        customSearcher = new CustomSearcher(indexWriter);
    }

    @Nested
    class BuildFilterQuery {

        @Test
        void should_remap_field() {
            // Given
            var filters = Map.<String, Object>of("field1", "value1");
            var remapFields = Map.of("field1", "field1_remap");

            // When
            var query = customSearcher.buildFilterQuery(filters, remapFields);

            // Then
            assertThat(query).map(Objects::toString).contains("field1_remap:value1");
        }

        @Test
        void should_remap_field_with_multiple_values() {
            // Given
            var filters = Map.<String, Object>of("field1", List.of("value1", "value2"));
            var remapFields = Map.of("field1", "field1_remap");

            // When
            var query = customSearcher.buildFilterQuery(filters, remapFields);

            // Then
            assertThat(query).map(Objects::toString).contains("field1_remap:value1 field1_remap:value2");
        }

        @Test
        void should_remap_one_field_among_other() {
            // Given
            var filters = Map.<String, Object>of("field1", "value1", "field2", "value2");
            var remapFields = Map.of("field1", "field1_remap");

            // When
            var query = customSearcher.buildFilterQuery(filters, remapFields).orElseThrow();

            // Then
            Pattern compile = Pattern.compile("(?<outerparenthesis>[^ ]+ [^ ]+) \\+\\((?<inerparenthesis>[^ ]+ [^ ]+)\\)");
            Matcher matcher = compile.matcher(query.toString());
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group("outerparenthesis")).contains("+field1_remap:value1").contains("+field2:value2");
            assertThat(matcher.group("inerparenthesis")).contains("+field1_remap:value1").contains("+field2:value2");
        }
    }
}
