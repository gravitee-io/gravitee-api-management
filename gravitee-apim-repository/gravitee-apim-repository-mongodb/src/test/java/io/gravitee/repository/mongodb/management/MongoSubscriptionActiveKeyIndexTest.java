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

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.repository.mongodb.management.internal.plan.SubscriptionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptions.ActiveKeyHashIndexUpgrader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The repository writes the hash only once the unique index on it can refuse duplicates: never while it is missing,
 * still being built, or defined otherwise, and no more than a minute after it is dropped.
 *
 * @author GraviteeSource Team
 */
public class MongoSubscriptionActiveKeyIndexTest extends AbstractManagementRepositoryTest {

    /**
     * Holds an index build after it started, until turned off: the index is listed but does not refuse duplicates.
     */
    private static final String HOLD_INDEX_BUILD = "hangAfterStartingIndexBuildUnlocked";

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    @Qualifier("indexManagementReactiveMongoTemplate")
    private ReactiveMongoOperations indexTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private Environment environment;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private SubscriptionMongoRepository internalSubscriptionRepository;

    private final AtomicLong now = new AtomicLong(System.currentTimeMillis());

    private String prefix;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Before
    public void init() {
        prefix = environment.getProperty("management.mongodb.prefix", "");
    }

    /**
     * Puts the expected index back for the other tests sharing the database, whatever the outcome of the test.
     */
    @After
    public void restoreIndex() {
        holdIndexBuilds(false);
        var index = index();
        if (index != null && !Boolean.TRUE.equals(index.getBoolean("unique"))) {
            subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);
        }
        createIndex();
    }

    @Test
    public void should_be_ready_once_the_index_is_complete() {
        assertThat(newGate().isReady()).isTrue();
    }

    @Test
    public void should_not_be_ready_while_the_index_does_not_exist() {
        subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);

        assertThat(newGate().isReady()).isFalse();
    }

    @Test
    public void should_not_be_ready_while_the_index_is_being_built() throws Exception {
        subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);
        aSubscriptionWrittenBeforeTheIndex();
        holdIndexBuilds(true);
        var build = CompletableFuture.runAsync(this::createIndex);
        waitUntil(() -> index() != null);
        var gate = newGate();

        // Listed, but a duplicate inserted now would only make the build fail at its end.
        assertThat(gate.isReady()).isFalse();

        holdIndexBuilds(false);
        build.get(30, TimeUnit.SECONDS);
        assertThat(gate.isReady()).as("before the next check").isFalse();
        now.addAndGet(MongoSubscriptionActiveKeyIndex.CHECK_INTERVAL.toMillis());
        assertThat(gate.isReady()).as("at the next check").isTrue();
    }

    @Test
    public void should_stop_being_ready_within_a_minute_of_the_index_being_dropped() {
        var gate = newGate();
        assertThat(gate.isReady()).isTrue();

        subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);

        now.addAndGet(MongoSubscriptionActiveKeyIndex.CHECK_INTERVAL.toMillis() - 1);
        assertThat(gate.isReady()).as("until the next check").isTrue();
        now.addAndGet(1);
        assertThat(gate.isReady()).as("at the next check").isFalse();
    }

    @Test
    public void should_not_be_ready_on_an_index_of_the_same_name_defined_otherwise() {
        subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);
        subscriptions().createIndex(
            new Document(MongoSubscriptionActiveKeyIndex.FIELD, 1),
            new IndexOptions().name(MongoSubscriptionActiveKeyIndex.NAME)
        );

        assertThat(newGate().isReady()).isFalse();
    }

    @Test
    public void should_be_ready_once_the_upgrader_has_created_the_index() {
        subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);
        aSubscriptionWrittenBeforeTheIndex();
        var upgrader = new ActiveKeyHashIndexUpgrader();
        upgrader.setEnvironment(environment);
        upgrader.setMongoTemplate(indexTemplate);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(newGate().isReady()).isTrue();
    }

    @Test
    public void should_write_no_hash_while_the_index_is_not_ready() throws Exception {
        subscriptions().dropIndex(MongoSubscriptionActiveKeyIndex.NAME);
        var repository = new MongoSubscriptionRepository();
        ReflectionTestUtils.setField(repository, "mapper", mapper);
        ReflectionTestUtils.setField(repository, "internalSubscriptionRepository", internalSubscriptionRepository);
        ReflectionTestUtils.setField(repository, "activeKeyIndex", newGate());

        repository.create(aSubscription("sub-1"));

        assertThat(subscriptions().find(Filters.eq("_id", "sub-1")).first()).doesNotContainKey(MongoSubscriptionActiveKeyIndex.FIELD);
    }

    /**
     * MongoDB creates an index on an empty collection at once, without any build to hold.
     */
    private void aSubscriptionWrittenBeforeTheIndex() {
        subscriptions().insertOne(new Document("_id", "sub-0").append("status", "CLOSED"));
    }

    private MongoSubscriptionActiveKeyIndex newGate() {
        return new MongoSubscriptionActiveKeyIndex(mongoOperations, prefix, now::get);
    }

    private void createIndex() {
        mongoClient
            .getDatabase(subscriptions().getNamespace().getDatabaseName())
            .runCommand(
                new Document("createIndexes", subscriptions().getNamespace().getCollectionName()).append(
                    "indexes",
                    List.of(MongoSubscriptionActiveKeyIndex.definition())
                )
            );
    }

    private void holdIndexBuilds(boolean hold) {
        mongoClient
            .getDatabase("admin")
            .runCommand(new Document("configureFailPoint", HOLD_INDEX_BUILD).append("mode", hold ? "alwaysOn" : "off"));
    }

    private Document index() {
        return subscriptions()
            .listIndexes()
            .into(new ArrayList<>())
            .stream()
            .filter(index -> MongoSubscriptionActiveKeyIndex.NAME.equals(index.getString("name")))
            .findFirst()
            .orElse(null);
    }

    private MongoCollection<Document> subscriptions() {
        return mongoOperations.getCollection(prefix + "subscriptions");
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within 30 seconds");
            }
            Thread.sleep(100);
        }
    }

    private static Subscription aSubscription(String id) {
        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEnvironmentId("DEFAULT");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api-1");
        subscription.setApi("api-1");
        subscription.setApplication("app-1");
        subscription.setPlan("plan-1");
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setCreatedAt(new Date());
        subscription.setUpdatedAt(new Date());
        return subscription;
    }
}
