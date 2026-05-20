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

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.ascending;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoSubscriptionPlanIndexTest extends AbstractManagementRepositoryTest {

    private static final String EXPECTED_INDEX_NAME = "p1ua1";

    @Inject
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    protected String getTestCasesPath() {
        return "/data/subscription-tests/";
    }

    /**
     * Mirrors the production query in
     * {@link io.gravitee.repository.mongodb.management.internal.plan.SubscriptionMongoRepositoryImpl#search}
     * for callers that pass a multi-value {@code plans} criterion and sort ascending on {@code updatedAt}
     * (e.g. {@code ApiProductSubscriptionRefresher.loadSubscriptionModels}, {@code SubscriptionFetcher.fetchLatest}).
     */
    @Test
    public void searchByPlanListSortedByUpdatedAtAsc_shouldUseCompoundIndex() {
        List<Bson> pipeline = List.of(match(in("plan", List.of("plan3", "plan4"))), sort(ascending("updatedAt")));

        Document explain = mongoTemplate.getCollection(tablePrefix + "subscriptions").aggregate(pipeline).explain();

        Optional<Document> winningPlan = findFirst(explain, "winningPlan");
        Optional<String> indexName = winningPlan.flatMap(plan -> findFirst(plan, "indexName")).map(Object::toString);

        assertThat(indexName)
            .as("winning plan should use the %s compound index. Explain: %s", EXPECTED_INDEX_NAME, explain.toJson())
            .contains(EXPECTED_INDEX_NAME);
    }

    /** Depth-first search for the first occurrence of {@code key} in {@code root} (a BSON document tree). */
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
