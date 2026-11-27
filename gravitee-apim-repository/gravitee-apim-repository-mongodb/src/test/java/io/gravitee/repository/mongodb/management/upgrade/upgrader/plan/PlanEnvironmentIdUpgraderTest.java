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
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

public class PlanEnvironmentIdUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private PlanEnvironmentIdUpgrader upgrader;
    private String plansCollection;
    private String apisCollection;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collections directly.
        return null;
    }

    @Before
    public void initUpgrader() {
        upgrader = new PlanEnvironmentIdUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        var prefix = environment.getProperty("management.mongodb.prefix", "");
        plansCollection = prefix + "plans";
        apisCollection = prefix + "apis";
        cleanUp();
        mongoTemplate.getCollection(apisCollection).insertOne(new Document("_id", "api-1").append("environmentId", "env-1"));
        mongoTemplate.getCollection(apisCollection).insertOne(new Document("_id", "api-2").append("environmentId", "env-2"));
    }

    @After
    public void cleanUp() {
        // deleteMany, not drop: drop() also removes the indexes created once at context bootstrap, which the
        // notablescan-guarded queries then have no plan to use.
        mongoTemplate.getCollection(plansCollection).deleteMany(new Document());
        mongoTemplate.getCollection(apisCollection).deleteMany(new Document());
    }

    private void givenPlan(Document plan) {
        mongoTemplate.getCollection(plansCollection).insertOne(plan);
    }

    private String environmentOf(String planId) {
        return mongoTemplate.getCollection(plansCollection).find(new Document("_id", planId)).first().getString("environmentId");
    }

    @Test
    public void should_set_environment_of_the_referenced_api_on_plans_without_environment() throws Exception {
        givenPlan(new Document("_id", "missing").append("referenceType", "API").append("referenceId", "api-1"));
        givenPlan(new Document("_id", "null").append("referenceType", "API").append("referenceId", "api-2").append("environmentId", null));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(environmentOf("missing")).isEqualTo("env-1");
        assertThat(environmentOf("null")).isEqualTo("env-2");
    }

    @Test
    public void should_use_the_api_field_when_plan_has_no_reference_type() throws Exception {
        givenPlan(new Document("_id", "legacy").append("api", "api-1"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(environmentOf("legacy")).isEqualTo("env-1");
    }

    @Test
    public void should_not_touch_plans_that_already_have_an_environment() throws Exception {
        givenPlan(
            new Document("_id", "plan-1").append("referenceType", "API").append("referenceId", "api-1").append("environmentId", "other")
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(environmentOf("plan-1")).isEqualTo("other");
    }

    @Test
    public void should_skip_plans_without_a_resolvable_api() throws Exception {
        givenPlan(new Document("_id", "product").append("referenceType", "API_PRODUCT").append("referenceId", "api-1"));
        givenPlan(new Document("_id", "unknown-api").append("referenceType", "API").append("referenceId", "deleted-api"));
        givenPlan(new Document("_id", "empty-api").append("referenceType", "API").append("referenceId", ""));
        givenPlan(new Document("_id", "no-api"));

        assertThat(upgrader.upgrade()).isTrue();

        for (String id : new String[] { "product", "unknown-api", "empty-api", "no-api" }) {
            assertThat(environmentOf(id)).as(id).isNull();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_not_overwrite_an_environment_set_by_another_node_between_the_read_and_the_write() throws Exception {
        givenPlan(new Document("_id", "plan-1").append("referenceType", "API").append("referenceId", "api-1"));
        var real = mongoTemplate.getCollection(plansCollection);
        MongoCollection<Document> racing = mock(MongoCollection.class, delegatesTo(real));
        doAnswer(invocation -> {
            real.updateOne(Filters.eq("_id", "plan-1"), Updates.set("environmentId", "set-meanwhile"));
            return real.bulkWrite(invocation.getArgument(0), invocation.<BulkWriteOptions>getArgument(1));
        })
            .when(racing)
            .bulkWrite(anyList(), any(BulkWriteOptions.class));

        assertThat(upgraderWritingPlansThrough(racing).upgrade()).isTrue();

        assertThat(environmentOf("plan-1")).isEqualTo("set-meanwhile");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_report_a_failure_when_an_earlier_batch_write_is_not_acknowledged() throws Exception {
        // three batches: only the first write is not acknowledged
        var plans = new ArrayList<Document>();
        for (int i = 0; i < PlanEnvironmentIdUpgrader.BATCH_SIZE * 2 + 1; i++) {
            plans.add(new Document("_id", "plan-" + i).append("referenceType", "API").append("referenceId", "api-1"));
        }
        mongoTemplate.getCollection(plansCollection).insertMany(plans);
        var real = mongoTemplate.getCollection(plansCollection);
        MongoCollection<Document> unacknowledgedFirst = mock(MongoCollection.class, delegatesTo(real));
        // a mocked BulkWriteResult reports wasAcknowledged() == false
        doReturn(mock(BulkWriteResult.class))
            .doAnswer(invocation -> real.bulkWrite(invocation.getArgument(0), invocation.<BulkWriteOptions>getArgument(1)))
            .when(unacknowledgedFirst)
            .bulkWrite(anyList(), any(BulkWriteOptions.class));

        assertThat(upgraderWritingPlansThrough(unacknowledgedFirst).upgrade()).isFalse();
    }

    private PlanEnvironmentIdUpgrader upgraderWritingPlansThrough(MongoCollection<Document> plans) {
        var spiedUpgrader = new PlanEnvironmentIdUpgrader() {
            @Override
            protected MongoCollection<Document> getCollection(String collectionName) {
                return "plans".equals(collectionName) ? plans : super.getCollection(collectionName);
            }
        };
        spiedUpgrader.setMongoTemplate(mongoTemplate);
        spiedUpgrader.setEnvironment(environment);
        return spiedUpgrader;
    }

    @Test
    public void should_succeed_on_empty_collection() throws Exception {
        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_upgrade_more_plans_than_a_single_batch() throws Exception {
        var plans = new ArrayList<Document>();
        var ids = new ArrayList<String>();
        for (int i = 0; i < PlanEnvironmentIdUpgrader.BATCH_SIZE * 2 + 1; i++) {
            plans.add(
                new Document("_id", "plan-" + i).append("referenceType", "API").append("referenceId", i % 2 == 0 ? "api-1" : "api-2")
            );
            ids.add("plan-" + i);
        }
        mongoTemplate.getCollection(plansCollection).insertMany(plans);
        var real = mongoTemplate.getCollection(plansCollection);
        MongoCollection<Document> spied = mock(MongoCollection.class, delegatesTo(real));

        assertThat(upgraderWritingPlansThrough(spied).upgrade()).isTrue();

        ArgumentCaptor<List<WriteModel<Document>>> writes = ArgumentCaptor.forClass(List.class);
        verify(spied, times(3)).bulkWrite(writes.capture(), any(BulkWriteOptions.class));
        assertThat(writes.getAllValues())
            .extracting(List::size)
            .containsExactly(PlanEnvironmentIdUpgrader.BATCH_SIZE, PlanEnvironmentIdUpgrader.BATCH_SIZE, 1);
        // queried by _id: the test MongoDB runs with notablescan
        var upgraded = mongoTemplate
            .getCollection(plansCollection)
            .find(new Document("_id", new Document("$in", ids)))
            .into(new ArrayList<>());
        assertThat(upgraded)
            .hasSize(ids.size())
            .allSatisfy(plan ->
                assertThat(plan.getString("environmentId")).isEqualTo(plan.getString("referenceId").equals("api-1") ? "env-1" : "env-2")
            );
    }
}
