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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

/**
 * Nodes of the previous versions keep writing subscriptions without maintaining their unique active key (a full
 * document replace drops it), so the key is repaired from the subscription fields themselves.
 *
 * @author GraviteeSource Team
 */
public class SubscriptionUniqueActiveKeyUpgraderTest extends AbstractManagementRepositoryTest {

    private static final String FIELD = "uniqueActiveKey";

    @Inject
    private SubscriptionUniqueActiveKeyUpgrader upgrader;

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    private Environment environment;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_add_the_key_of_an_active_subscription_written_without_it() {
        subscriptions().insertOne(aSubscriptionDocument("sub-1", "ACCEPTED", "plan-1"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(storedKey("sub-1")).isEqualTo(expectedKey("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
    }

    @Test
    public void should_remove_the_key_left_on_a_closed_or_rejected_subscription() {
        subscriptions().insertOne(aSubscriptionDocument("sub-closed", "CLOSED", "plan-1").append(FIELD, "API:api-1:app-1:plan-1"));
        subscriptions().insertOne(aSubscriptionDocument("sub-rejected", "REJECTED", "plan-1").append(FIELD, "API:api-1:app-1:plan-1"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(storedDocument("sub-closed")).doesNotContainKey(FIELD);
        assertThat(storedDocument("sub-rejected")).doesNotContainKey(FIELD);
    }

    @Test
    public void should_replace_a_key_computed_before_the_subscription_changed() {
        // Transferred to plan-2 by a node of the previous version, which kept the key of plan-1.
        subscriptions().insertOne(aSubscriptionDocument("sub-1", "ACCEPTED", "plan-2").append(FIELD, "API:api-1:app-1:plan-1"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(storedKey("sub-1")).isEqualTo(expectedKey("sub-1", Subscription.Status.ACCEPTED, "plan-2"));
    }

    @Test
    public void should_repair_the_key_of_a_push_subscription_from_its_channel() {
        subscriptions().insertOne(
            aSubscriptionDocument("sub-1", "ACCEPTED", "plan-1").append("type", "PUSH").append("configuration", "{\"channel\":\"orders\"}")
        );

        assertThat(upgrader.upgrade()).isTrue();

        var expected = SubscriptionUniqueActiveKey.of(
            aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1")
                .type(Subscription.Type.PUSH)
                .configuration("{\"channel\":\"orders\"}")
                .build()
        );
        assertThat(storedKey("sub-1")).isEqualTo(expected);
    }

    @Test
    public void should_repair_more_subscriptions_than_a_single_batch() {
        int count = SubscriptionUniqueActiveKeyRepair.BATCH_SIZE * 2 + 7;
        List<Document> documents = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> documents.add(aSubscriptionDocument("sub-" + i, "ACCEPTED", "plan-" + i)));
        subscriptions().insertMany(documents);

        assertThat(upgrader.upgrade()).isTrue();

        // The test database refuses collection scans (notablescan).
        assertThat(subscriptions().countDocuments(Filters.exists(FIELD), new CountOptions().hint(new Document("_id", 1)))).isEqualTo(count);
        assertThat(storedKey("sub-" + (count - 1))).isEqualTo(
            expectedKey("sub-" + (count - 1), Subscription.Status.ACCEPTED, "plan-" + (count - 1))
        );
    }

    @Test
    public void should_give_the_same_result_when_run_again() {
        subscriptions().insertOne(aSubscriptionDocument("sub-1", "ACCEPTED", "plan-1"));
        subscriptions().insertOne(aSubscriptionDocument("sub-2", "CLOSED", "plan-1").append(FIELD, "stale"));
        upgrader.upgrade();
        var firstRun = List.of(storedDocument("sub-1"), storedDocument("sub-2"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(List.of(storedDocument("sub-1"), storedDocument("sub-2"))).isEqualTo(firstRun);
    }

    @Test
    public void should_give_the_same_result_when_two_nodes_repair_at_the_same_time() throws Exception {
        int count = SubscriptionUniqueActiveKeyRepair.BATCH_SIZE * 2 + 7;
        List<Document> documents = new ArrayList<>();
        IntStream.range(0, count).forEach(i -> documents.add(aSubscriptionDocument("sub-" + i, "ACCEPTED", "plan-" + i)));
        subscriptions().insertMany(documents);
        var barrier = new CyclicBarrier(2);
        ExecutorService nodes = Executors.newFixedThreadPool(2);
        List<SubscriptionUniqueActiveKeyRepair.Result> results = new ArrayList<>();
        try {
            for (Future<SubscriptionUniqueActiveKeyRepair.Result> result : nodes.invokeAll(
                List.of(aRepairStartingWith(barrier), aRepairStartingWith(barrier))
            )) {
                results.add(result.get());
            }
        } finally {
            nodes.shutdownNow();
        }

        assertThat(results).allSatisfy(result -> assertThat(result.conflicts()).isZero());
        // A key the other node has already written is matched again but not modified.
        assertThat(results.stream().mapToLong(SubscriptionUniqueActiveKeyRepair.Result::fixed).sum()).isEqualTo(count);
        assertThat(subscriptions().countDocuments(Filters.exists(FIELD), new CountOptions().hint(new Document("_id", 1)))).isEqualTo(count);
        assertThat(storedKey("sub-" + (count - 1))).isEqualTo(
            expectedKey("sub-" + (count - 1), Subscription.Status.ACCEPTED, "plan-" + (count - 1))
        );
    }

    @Test
    public void should_not_stop_the_node_when_the_repair_fails() {
        var unreachable = mock(ReactiveMongoOperations.class);
        when(unreachable.getCollection(anyString())).thenReturn(Mono.error(new MongoTimeoutException("timed out")));
        var failing = new SubscriptionUniqueActiveKeyUpgrader();
        failing.setEnvironment(environment);
        failing.setMongoTemplate(unreachable);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(SubscriptionUniqueActiveKeyUpgrader.class);
        logs.start();
        logger.addAppender(logs);
        try {
            assertThat(failing.upgrade()).isTrue();
        } finally {
            logger.detachAppender(logs);
        }

        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getThrowableProxy().getClassName()).isEqualTo(MongoTimeoutException.class.getName());
        });
    }

    @Test
    public void should_leave_a_subscription_changed_during_the_repair_to_its_writer() {
        subscriptions().insertOne(aSubscriptionDocument("sub-1", "ACCEPTED", "plan-1"));
        var repair = new SubscriptionUniqueActiveKeyRepair(upgrader.subscriptionsCollection());
        var read = repair.readBatch(null);
        // Closed by another node between the read and the write of the repair.
        subscriptions().updateOne(Filters.eq("_id", "sub-1"), new Document("$set", new Document("status", "CLOSED")));

        repair.repairBatch(read);

        assertThat(storedDocument("sub-1")).doesNotContainKey(FIELD);
    }

    @Test
    public void should_report_the_subscription_whose_key_another_active_subscription_already_holds() {
        // Once the unique index exists, the key a subscription misses may already be held. The subscription reported is
        // the one whose key could not be written, whatever the up-to-date subscriptions read before it.
        // The tests share the database: an index that already exists is left in place for the others.
        boolean indexAlreadyExists = StreamSupport.stream(subscriptions().listIndexes().spliterator(), false).anyMatch(index ->
            "uak1_unique".equals(index.getString("name"))
        );
        subscriptions().createIndex(
            new Document(FIELD, 1),
            new IndexOptions().name("uak1_unique").unique(true).partialFilterExpression(new Document(FIELD, new Document("$exists", true)))
        );
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(SubscriptionUniqueActiveKeyRepair.class);
        logs.start();
        logger.addAppender(logs);
        try {
            subscriptions().insertOne(
                aSubscriptionDocument("sub-a", "ACCEPTED", "plan-a").append(
                    FIELD,
                    expectedKey("sub-a", Subscription.Status.ACCEPTED, "plan-a")
                )
            );
            subscriptions().insertOne(aSubscriptionDocument("sub-b", "ACCEPTED", "plan-1"));
            subscriptions().insertOne(
                aSubscriptionDocument("sub-c", "ACCEPTED", "plan-1").append(
                    FIELD,
                    expectedKey("sub-c", Subscription.Status.ACCEPTED, "plan-1")
                )
            );

            var result = new SubscriptionUniqueActiveKeyRepair(upgrader.subscriptionsCollection()).repair();

            assertThat(result.conflicts()).isEqualTo(1);
            assertThat(logs.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("[sub-b]"))
                .noneMatch(message -> message.contains("[sub-a]"));
        } finally {
            logger.detachAppender(logs);
            if (!indexAlreadyExists) {
                subscriptions().dropIndex("uak1_unique");
            }
        }
    }

    private Callable<SubscriptionUniqueActiveKeyRepair.Result> aRepairStartingWith(CyclicBarrier barrier) {
        return () -> {
            barrier.await();
            return new SubscriptionUniqueActiveKeyRepair(upgrader.subscriptionsCollection()).repair();
        };
    }

    private String storedKey(String id) {
        return storedDocument(id).getString(FIELD);
    }

    private Document storedDocument(String id) {
        return subscriptions().find(Filters.eq("_id", id)).first();
    }

    private MongoCollection<Document> subscriptions() {
        return mongoOperations.getCollection(environment.getProperty("management.mongodb.prefix", "") + "subscriptions");
    }

    private static String expectedKey(String id, Subscription.Status status, String plan) {
        return SubscriptionUniqueActiveKey.of(aSubscription(id, status, plan).build());
    }

    private static Subscription.SubscriptionBuilder aSubscription(String id, Subscription.Status status, String plan) {
        return Subscription.builder()
            .id(id)
            .referenceType(SubscriptionReferenceType.API)
            .referenceId("api-1")
            .api("api-1")
            .application("app-1")
            .plan(plan)
            .status(status)
            .type(Subscription.Type.STANDARD);
    }

    private static Document aSubscriptionDocument(String id, String status, String plan) {
        return new Document("_id", id)
            .append("environmentId", "DEFAULT")
            .append("referenceType", "API")
            .append("referenceId", "api-1")
            .append("api", "api-1")
            .append("application", "app-1")
            .append("plan", plan)
            .append("status", status)
            .append("type", "STANDARD");
    }
}
