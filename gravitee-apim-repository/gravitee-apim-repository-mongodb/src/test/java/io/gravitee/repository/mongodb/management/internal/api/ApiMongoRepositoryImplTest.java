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
package io.gravitee.repository.mongodb.management.internal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.repository.management.api.search.ApiCriteria;
import java.util.List;
import java.util.regex.Pattern;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class ApiMongoRepositoryImplTest {

    @Nested
    class FillQuery {

        @Captor
        private ArgumentCaptor<CriteriaDefinition> criteriaDefinition;

        @Test
        void should_not_update_query_if_no_criteria() {
            // Given
            Query query = mock(Query.class);
            ApiCriteria criteria = new ApiCriteria.Builder().build();
            List<ApiCriteria> criterias = List.of(criteria);

            // When
            ApiMongoRepositoryImpl.fillQuery(query, criterias);

            // Then
            verifyNoInteractions(query);
        }

        @Test
        void filterName() {
            // Given
            Query query = mock(Query.class);
            ApiCriteria criteria = new ApiCriteria.Builder().filterName("my-name").build();

            // When
            ApiMongoRepositoryImpl.fillQuery(query, List.of(criteria));

            // Then
            verify(query).addCriteria(criteriaDefinition.capture());
            var definition = criteriaDefinition.getValue().getCriteriaObject();
            assertThat(definition).containsOnlyKeys("name");
            var pattern = (Pattern) definition.get("name");
            assertThat(pattern)
                .is(matcherOf("my-name"))
                .is(matcherOf("my-Name"))
                .is(matcherOf("something my-name"))
                .is(matcherOf("my-name something"))
                .isNot(matcherOf("name"));
        }

        private Condition<Pattern> matcherOf(String s) {
            return new Condition<>(pattern -> pattern.asPredicate().test(s), "match '%s'", s);
        }
    }
}
