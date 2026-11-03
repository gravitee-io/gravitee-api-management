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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * The repository reads {@code subscriptions} into a {@link java.util.Set}, which hides duplicated ids: these tests look at
 * the stored array itself.
 */
public class MongoApiKeySubscriptionsTest extends AbstractManagementRepositoryTest {

    private static final String KEY_ID = "id-of-shared-apikey";

    /**
     * The template built from the mapping context of the application, as in production: it resolves the collection name of
     * {@link ApiKeyMongo}, prefix included. The primary template of the tests cannot.
     */
    @Inject
    @Named("mongoTemplate")
    private MongoTemplate mongoTemplate;

    @Override
    protected String getTestCasesPath() {
        return "/data/apikey-tests/";
    }

    @Test
    public void addSubscription_should_store_a_subscription_added_twice_once() throws Exception {
        givenKey("sub-initial");

        apiKeyRepository.addSubscription(KEY_ID, "sub-retried");
        apiKeyRepository.addSubscription(KEY_ID, "sub-retried");
        apiKeyRepository.addSubscription(KEY_ID, "sub-initial");

        assertThat(storedSubscriptions()).containsExactly("sub-initial", "sub-retried");
    }

    private void givenKey(String subscription) throws Exception {
        apiKeyRepository.create(
            ApiKey.builder()
                .id(KEY_ID)
                .key("shared-key-value")
                .environmentId("DEFAULT")
                .application("shared-app")
                .subscriptions(new ArrayList<>(List.of(subscription)))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build()
        );
    }

    private List<String> storedSubscriptions() {
        Document stored = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ApiKeyMongo.class)).find(eq("_id", KEY_ID)).first();
        assertThat(stored).isNotNull();
        return stored.getList("subscriptions", String.class);
    }
}
