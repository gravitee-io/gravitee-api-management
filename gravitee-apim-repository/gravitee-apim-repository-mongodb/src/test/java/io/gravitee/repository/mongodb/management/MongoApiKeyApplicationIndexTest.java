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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoCollection;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoApiKeyApplicationIndexTest extends AbstractManagementRepositoryTest {

    private static final String EXPECTED_INDEX_NAME = "a1ua-1";

    /** Owns 3 of the API keys of the test data set. */
    private static final String APPLICATION_ID = "app4";

    @Inject
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    protected String getTestCasesPath() {
        return "/data/apikey-tests/";
    }

    /**
     * Mirrors the production query of
     * {@link io.gravitee.repository.mongodb.management.internal.key.ApiKeyMongoRepository#findByApplication}:
     * filter on {@code application}, newest {@code updatedAt} first.
     */
    @Test
    public void findByApplication_shouldOnlyReadTheKeysOfTheApplication() {
        Document explain = keys()
            .find(eq("application", APPLICATION_ID))
            .sort(descending("updatedAt"))
            .explain(ExplainVerbosity.EXECUTION_STATS);

        Optional<String> indexName = findFirst(explain.get("queryPlanner"), "winningPlan")
            .flatMap(plan -> findFirst(plan, "indexName"))
            .map(Object::toString);
        Document stats = explain.get("executionStats", Document.class);

        assertThat(indexName)
            .as("winning plan should use the %s index. Explain: %s", EXPECTED_INDEX_NAME, explain.toJson())
            .contains(EXPECTED_INDEX_NAME);
        assertThat(stats.getInteger("nReturned")).isEqualTo(3);
        assertThat(stats.getInteger("totalKeysExamined"))
            .as("only the keys of the application should be examined. Explain: %s", explain.toJson())
            .isEqualTo(stats.getInteger("nReturned"));
        assertThat(stats.getInteger("totalDocsExamined")).isEqualTo(stats.getInteger("nReturned"));
    }

    /**
     * The new index is added next to the existing ones. {@code a1} is built on {@code applications}, a field that does
     * not exist, but it is kept as is (see
     * {@link io.gravitee.repository.mongodb.management.upgrade.upgrader.index.keys.ApplicationUpdatedAtIndexUpgrader}).
     */
    @Test
    public void keysIndexes_shouldAddApplicationUpdatedAtIndexAndKeepEveryExistingIndex() {
        Map<String, List<Map.Entry<String, Object>>> keySpecByIndexName = StreamSupport.stream(
            keys().listIndexes().spliterator(),
            false
        ).collect(Collectors.toMap(index -> index.getString("name"), index -> keySpec(index.get("key", Document.class))));

        assertThat(keySpecByIndexName).containsAllEntriesOf(
            Map.ofEntries(
                Map.entry("_id_", keySpec("{_id: 1}")),
                Map.entry("a1", keySpec("{applications: 1}")),
                Map.entry("e1", keySpec("{environmentId: 1}")),
                Map.entry("e1ua1i1", keySpec("{environmentId: 1, updatedAt: 1, _id: 1}")),
                Map.entry("f1", keySpec("{federated: 1}")),
                Map.entry("k1", keySpec("{key: 1}")),
                Map.entry("p1", keySpec("{plan: 1}")),
                Map.entry("r1", keySpec("{revoked: 1}")),
                Map.entry("r1ea1", keySpec("{revoked: 1, expireAt: 1}")),
                Map.entry("r1ua-1ea1", keySpec("{revoked: 1, updatedAt: -1, expireAt: 1}")),
                Map.entry("s1", keySpec("{subscriptions: 1}")),
                Map.entry("u1", keySpec("{updatedAt: 1}")),
                Map.entry(EXPECTED_INDEX_NAME, keySpec("{application: 1, updatedAt: -1}"))
            )
        );
    }

    private MongoCollection<Document> keys() {
        return mongoTemplate.getCollection(tablePrefix + "keys");
    }

    private static List<Map.Entry<String, Object>> keySpec(String json) {
        return keySpec(Document.parse(json));
    }

    /**
     * An index key spec as its ordered (field, direction) pairs. Numeric directions are compared by value, so {@code 1},
     * {@code 1.0} and {@code NumberLong(1)} are equal; other directions (for instance {@code "text"}) are kept as is.
     */
    private static List<Map.Entry<String, Object>> keySpec(Document key) {
        return key
            .entrySet()
            .stream()
            .map(field ->
                Map.entry(field.getKey(), field.getValue() instanceof Number direction ? direction.doubleValue() : field.getValue())
            )
            .toList();
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
