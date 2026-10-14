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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.plan;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PlanReferenceTypeUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private PlanReferenceTypeUpgrader upgrader;
    private String collectionName;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collection directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new PlanReferenceTypeUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        collectionName = environment.getProperty("management.mongodb.prefix", "") + "plans";
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
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "plan-1").append("api", "api-1"));

        assertThat(upgrader.upgrade()).isTrue();

        var plan = find("plan-1");
        assertThat(plan.getString("referenceType")).isEqualTo("API");
        assertThat(plan.getString("referenceId")).isEqualTo("api-1");
        assertThat(plan.getString("api")).isEqualTo("api-1");
    }

    @Test
    public void should_set_reference_type_and_id_when_reference_type_is_null() throws Exception {
        mongoTemplate
            .getCollection(collectionName)
            .insertOne(new Document("_id", "plan-1").append("api", "api-1").append("referenceType", null));

        assertThat(upgrader.upgrade()).isTrue();

        var plan = find("plan-1");
        assertThat(plan.getString("referenceType")).isEqualTo("API");
        assertThat(plan.getString("referenceId")).isEqualTo("api-1");
    }

    @Test
    public void should_not_touch_plans_that_already_have_a_reference() throws Exception {
        mongoTemplate
            .getCollection(collectionName)
            .insertOne(new Document("_id", "plan-1").append("api", "api-1").append("referenceType", "API").append("referenceId", "other"));
        mongoTemplate
            .getCollection(collectionName)
            .insertOne(new Document("_id", "plan-2").append("referenceType", "API_PRODUCT").append("referenceId", "product-1"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(find("plan-1").getString("referenceId")).isEqualTo("other");
        var product = find("plan-2");
        assertThat(product.getString("referenceType")).isEqualTo("API_PRODUCT");
        assertThat(product.getString("referenceId")).isEqualTo("product-1");
    }

    @Test
    public void should_skip_plans_without_a_usable_api() throws Exception {
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "no-api"));
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "null-api").append("api", null));
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "empty-api").append("api", ""));

        assertThat(upgrader.upgrade()).isTrue();

        for (String id : new String[] { "no-api", "null-api", "empty-api" }) {
            assertThat(find(id).containsKey("referenceType")).as(id).isFalse();
            assertThat(find(id).containsKey("referenceId")).as(id).isFalse();
        }
    }

    @Test
    public void should_succeed_on_empty_or_missing_collection() throws Exception {
        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    public void should_be_idempotent() throws Exception {
        mongoTemplate.getCollection(collectionName).insertOne(new Document("_id", "plan-1").append("api", "api-1"));

        assertThat(upgrader.upgrade()).isTrue();
        assertThat(upgrader.upgrade()).isTrue();

        var plan = find("plan-1");
        assertThat(plan.getString("referenceType")).isEqualTo("API");
        assertThat(plan.getString("referenceId")).isEqualTo("api-1");
    }

    @Test
    public void should_upgrade_more_plans_than_a_single_batch() throws Exception {
        var plans = new ArrayList<Document>();
        var ids = new ArrayList<String>();
        for (int i = 0; i < 2500; i++) {
            plans.add(new Document("_id", "plan-" + i).append("api", "api-" + i));
            ids.add("plan-" + i);
        }
        mongoTemplate.getCollection(collectionName).insertMany(plans);

        assertThat(upgrader.upgrade()).isTrue();

        // queried by _id: the test MongoDB runs with notablescan
        var upgraded = mongoTemplate
            .getCollection(collectionName)
            .find(new Document("_id", new Document("$in", ids)))
            .into(new ArrayList<>());
        assertThat(upgraded).hasSize(2500);
        assertThat(upgraded).allSatisfy(plan -> {
            assertThat(plan.getString("referenceType")).isEqualTo("API");
            assertThat(plan.getString("referenceId")).isEqualTo(plan.getString("api"));
        });
    }
}
