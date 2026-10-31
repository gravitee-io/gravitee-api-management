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
package io.gravitee.repository.mongodb.management;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Sorts.ascending;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.mongodb.management.internal.eventLatest.event.EventLatestMongoRepositoryImpl;
import jakarta.inject.Inject;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

public class MongoEventLatestSeekIndexTest extends AbstractManagementRepositoryTest {

    private static final String SEEK_INDEX_NAME = "t1u1i1";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private EventLatestMongoRepositoryImpl eventLatestMongoRepositoryImpl;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    protected String getTestCasesPath() {
        return "/data/event-latest-tests/";
    }

    @Test
    public void should_serve_a_keyset_page_from_the_seek_index() {
        Document explain = events()
            .find(and(exists("properties.api_id"), in("type", "START_API"), ne("updatedAt", null)))
            .sort(ascending("updatedAt", "_id"))
            .limit(2)
            .explain();

        Optional<String> indexName = findFirst(explain, "winningPlan")
            .flatMap(plan -> findFirst(plan, "indexName"))
            .map(Object::toString);

        assertThat(indexName)
            .as("winning plan should use the %s index. Explain: %s", SEEK_INDEX_NAME, explain.toJson())
            .contains(SEEK_INDEX_NAME);
    }

    @Test
    public void should_refuse_to_seek_without_the_seek_index() {
        events().dropIndex(SEEK_INDEX_NAME);
        try {
            ReflectionTestUtils.setField(eventLatestMongoRepositoryImpl, "seekIndexFound", false);

            assertThatThrownBy(() ->
                eventLatestRepository.searchAfter(EventCriteria.builder().build(), Event.EventProperties.API_ID, null, 2)
            ).isInstanceOf(UnsupportedOperationException.class);
        } finally {
            events().createIndex(new Document("type", 1).append("updatedAt", 1).append("_id", 1), new IndexOptions().name(SEEK_INDEX_NAME));
        }
    }

    private MongoCollection<Document> events() {
        return mongoTemplate.getCollection(tablePrefix + "events_latest");
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> findFirst(Object root, String key) {
        if (root instanceof Document doc) {
            Object direct = doc.get(key);
            if (direct != null) {
                return Optional.of((T) direct);
            }
            for (Object child : doc.values()) {
                Optional<T> nested = findFirst(child, key);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        } else if (root instanceof Iterable<?> iterable) {
            for (Object child : iterable) {
                Optional<T> nested = findFirst(child, key);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }
}
