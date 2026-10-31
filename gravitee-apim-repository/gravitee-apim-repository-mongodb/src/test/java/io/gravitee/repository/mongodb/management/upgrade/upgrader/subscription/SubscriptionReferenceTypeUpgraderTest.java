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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SubscriptionReferenceTypeUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private SubscriptionReferenceTypeUpgrader upgrader;
    private String collectionName;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collection directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new SubscriptionReferenceTypeUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        collectionName = environment.getProperty("management.mongodb.prefix", "") + "subscriptions";
        // deleteMany, not drop: drop() also removes the indexes created once at context bootstrap, which the
        // notablescan-guarded queries then have no plan to use.
        mongoTemplate.getCollection(collectionName).deleteMany(new Document());
    }

    @AfterEach
    public void cleanUp() {
        mongoTemplate.getCollection(collectionName).deleteMany(new Document());
    }

    private Document find(String id) {
        return mongoTemplate.getCollection(collectionName).find(new Document("_id", id)).first();
    }

    @Test
    public void should_set_reference_type_and_id_from_api_when_missing() throws Exception {
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "sub-1").append("api", "api-1").append("plan", "plan-1"));

        assertThat(upgrader.upgrade()).isTrue();

        var subscription = find("sub-1");
        assertThat(subscription.getString("referenceType")).isEqualTo("API");
        assertThat(subscription.getString("referenceId")).isEqualTo("api-1");
        assertThat(subscription.getString("plan")).isEqualTo("plan-1");
    }

    @Test
    public void should_not_touch_subscriptions_that_already_have_a_reference() throws Exception {
        mongoTemplate
            .getCollection(collectionName)
            .insertOne(new Document("_id", "sub-1").append("api", "api-1").append("referenceType", "API").append("referenceId", "other"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(find("sub-1").getString("referenceId")).isEqualTo("other");
    }

    @Test
    public void should_skip_subscriptions_without_a_usable_api() throws Exception {
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "no-api"));
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "null-api").append("api", null));
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "empty-api").append("api", ""));

        assertThat(upgrader.upgrade()).isTrue();

        for (String id : new String[] { "no-api", "null-api", "empty-api" }) {
            assertThat(find(id).containsKey("referenceType")).as(id).isFalse();
        }
    }

    @Test
    public void should_succeed_on_empty_or_missing_collection() throws Exception {
        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    public void should_be_idempotent() throws Exception {
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "sub-1").append("api", "api-1"));

        assertThat(upgrader.upgrade()).isTrue();
        assertThat(upgrader.upgrade()).isTrue();

        assertThat(find("sub-1").getString("referenceId")).isEqualTo("api-1");
    }
}
