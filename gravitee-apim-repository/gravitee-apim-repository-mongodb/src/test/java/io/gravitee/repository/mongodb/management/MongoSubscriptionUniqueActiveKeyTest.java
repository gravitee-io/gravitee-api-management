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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * The hash of the unique active key is a storage detail of the subscriptions collection, never exposed by the
 * repository model, hence a MongoDB specific test reading the stored documents.
 *
 * @author GraviteeSource Team
 */
public class MongoSubscriptionUniqueActiveKeyTest extends AbstractManagementRepositoryTest {

    private static final String FIELD = "activeKeyHash";
    private static final String INDEX = "akh1_unique";
    /**
     * The key itself, maintained by 4.12.26 only: nothing reads it, and its values may be duplicated or stale.
     */
    private static final String PREVIOUS_FIELD = "uniqueActiveKey";

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    private Environment environment;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private MongoClient mongoClient;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_store_the_hash_of_the_unique_active_key_of_an_active_subscription() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_not_store_the_unique_active_key_itself_anymore() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));

        assertThat(storedDocument("sub-1")).doesNotContainKey(PREVIOUS_FIELD);
    }

    @Test
    public void should_not_store_any_hash_for_a_rejected_or_closed_subscription() throws Exception {
        subscriptionRepository.create(aSubscription("sub-rejected", Subscription.Status.REJECTED));
        subscriptionRepository.create(aSubscription("sub-closed", Subscription.Status.CLOSED));

        // A null value would still be matched by the partial filter of the unique index, the field must be absent.
        assertThat(storedDocument("sub-rejected")).doesNotContainKey(FIELD);
        assertThat(storedDocument("sub-closed")).doesNotContainKey(FIELD);
    }

    @Test
    public void should_remove_the_hash_when_the_subscription_is_closed() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));

        subscription.setStatus(Subscription.Status.CLOSED);
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1")).doesNotContainKey(FIELD);
    }

    @Test
    public void should_keep_the_hash_while_the_subscription_is_paused() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        var hash = storedDocument("sub-1").getString(FIELD);

        subscription.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(hash);
    }

    @Test
    public void should_recompute_the_hash_when_the_subscription_is_transferred_to_another_plan() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        var hashOnPlan1 = storedDocument("sub-1").getString(FIELD);

        subscription.setPlan("plan-2");
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotEqualTo(hashOnPlan1).isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_give_the_hash_back_when_a_closed_subscription_is_restored() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.CLOSED));

        subscription.setStatus(Subscription.Status.PENDING);
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_recompute_the_hash_when_the_channel_of_a_push_subscription_changes() throws Exception {
        var subscription = aSubscription("sub-1", Subscription.Status.ACCEPTED);
        subscription.setType(Subscription.Type.PUSH);
        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"orders\"}");
        subscription = subscriptionRepository.create(subscription);
        var hashOnOrders = storedDocument("sub-1").getString(FIELD);

        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"invoices\"}");
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotEqualTo(hashOnOrders).isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_create_a_unique_index_on_the_hash_of_the_active_subscriptions_only_at_startup() {
        var index = index(INDEX);

        assertThat(index).isNotNull();
        assertThat(index.getBoolean("unique")).isTrue();
        assertThat(index.get("key", Document.class)).isEqualTo(new Document(FIELD, 1));
        assertThat(index.get("partialFilterExpression", Document.class)).isEqualTo(new Document(FIELD, new Document("$exists", true)));
    }

    /**
     * The upgraders run at startup, before the node serves requests: the index is built on a field no existing
     * subscription has, so building it must not read them.
     */
    @Test
    public void should_not_read_the_subscriptions_when_creating_the_index() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        subscriptions().dropIndex(INDEX);
        var database = database();
        database.runCommand(new Document("profile", 0));
        database.getCollection("system.profile").drop();
        database.runCommand(new Document("profile", 2));
        List<Document> reads;
        try {
            try {
                activeKeyHashIndexUpgrader().upgrade();
            } finally {
                database.runCommand(new Document("profile", 0));
            }

            reads = database
                .getCollection("system.profile")
                .find(
                    Filters.and(
                        Filters.eq("ns", database.getName() + "." + collectionName("subscriptions")),
                        Filters.or(
                            Filters.in("op", "query", "getmore"),
                            Filters.exists("command.aggregate"),
                            Filters.exists("command.count"),
                            Filters.exists("command.distinct")
                        )
                    )
                )
                .into(new ArrayList<>());
        } finally {
            // The tests delete every document of every collection in between, which system.profile refuses.
            database.getCollection("system.profile").drop();
        }
        assertThat(reads).isEmpty();
        assertThat(index(INDEX)).isNotNull();
    }

    /**
     * Without the index, two identical requests could store the same hash, and the index could not be created anymore:
     * the node does not start without it, and creates it at its next startup.
     */
    @Test
    public void should_stop_the_node_when_the_index_cannot_be_created_and_create_it_at_the_next_startup() throws Exception {
        subscriptions().dropIndex(INDEX);
        // Same name, other options: the server refuses to create the expected index.
        subscriptions().createIndex(new Document(FIELD, 1), new IndexOptions().name(INDEX));
        boolean firstStartup;
        try {
            firstStartup = activeKeyHashIndexUpgrader().upgrade();
        } finally {
            subscriptions().dropIndex(INDEX);
        }

        assertThat(firstStartup).isFalse();
        assertThat(activeKeyHashIndexUpgrader().upgrade()).isTrue();
        assertThat(index(INDEX).getBoolean("unique")).isTrue();
    }

    /**
     * The upgraders run at startup, before the node serves requests: a repair would scan every subscription.
     */
    @Test
    public void should_not_repair_the_hashes_of_the_existing_subscriptions_at_startup() throws Exception {
        // Without its application, the subscription would be removed as orphaned.
        collection("applications").insertOne(new Document("_id", "app-1").append("environmentId", "DEFAULT").append("name", "app-1"));
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        writtenByPreviousVersion("sub-1");

        List<Upgrader> upgraders = applicationContext
            .getBeansOfType(Upgrader.class)
            .values()
            .stream()
            .sorted(Comparator.comparingInt(Upgrader::getOrder))
            .toList();
        // Collection scans are allowed at startup, unlike in the other tests: a repair must not get by with failing.
        setNoTableScan(false);
        try {
            for (Upgrader upgrader : upgraders) {
                upgrader.upgrade();
            }
        } finally {
            setNoTableScan(true);
        }

        assertThat(storedDocument("sub-1")).isNotNull().doesNotContainKey(FIELD);
    }

    @Test
    public void should_write_the_hash_of_a_subscription_stored_without_it_the_next_time_it_is_saved() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        writtenByPreviousVersion("sub-1");
        var subscription = subscriptionRepository.findById("sub-1").orElseThrow();

        subscription.setUpdatedAt(new Date());
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_not_refuse_a_subscription_because_of_a_subscription_written_before_the_hash() throws Exception {
        // The duplicates created before the index are left as they are: only the subscription service refuses a
        // subscription to a plan the application already subscribed.
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        writtenByPreviousVersion("sub-1");

        var subscription = subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED));

        assertThat(storedDocument("sub-2").getString(FIELD)).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_keep_updating_a_subscription_written_before_the_hash_while_another_one_holds_it() throws Exception {
        // Two active subscriptions of an application to a plan, duplicated before the unique index existed.
        var first = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-2"));
        subscriptions().updateOne(Filters.eq("_id", "sub-2"), new Document("$set", new Document("plan", "plan-1")));
        writtenByPreviousVersion("sub-1");
        writtenByPreviousVersion("sub-2");
        first.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(first);
        var second = subscriptionRepository.findById("sub-2").orElseThrow();

        second.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(second);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(hashOf(first));
        assertThat(storedDocument("sub-2")).doesNotContainKey(FIELD).containsEntry("status", "PAUSED");
    }

    /**
     * The repository bridge serialises the repository model for the gateways, the hash must stay out of it.
     */
    @Test
    public void should_keep_the_hash_out_of_the_repository_model() {
        assertThat(Subscription.class.getDeclaredFields()).noneMatch(
            field -> field.getName().equalsIgnoreCase(FIELD) || field.getName().equalsIgnoreCase(PREVIOUS_FIELD)
        );
    }

    /**
     * As stored by a node of a previous version, which knows nothing of the hash.
     */
    private void writtenByPreviousVersion(String id) {
        subscriptions().updateOne(Filters.eq("_id", id), new Document("$unset", new Document(FIELD, "")));
    }

    private Upgrader activeKeyHashIndexUpgrader() {
        return applicationContext
            .getBeansOfType(Upgrader.class)
            .values()
            .stream()
            .filter(upgrader -> upgrader.getClass().getSimpleName().startsWith("ActiveKeyHashIndexUpgrader"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No upgrader creates the index " + INDEX));
    }

    private void setNoTableScan(boolean enabled) {
        mongoClient.getDatabase("admin").runCommand(new Document("setParameter", 1).append("notablescan", enabled));
    }

    private Document index(String name) {
        return subscriptions()
            .listIndexes()
            .into(new ArrayList<>())
            .stream()
            .filter(index -> name.equals(index.getString("name")))
            .findFirst()
            .orElse(null);
    }

    private Document storedDocument(String id) {
        return subscriptions().find(Filters.eq("_id", id)).first();
    }

    private MongoCollection<Document> subscriptions() {
        return collection("subscriptions");
    }

    private MongoCollection<Document> collection(String name) {
        return mongoOperations.getCollection(collectionName(name));
    }

    private String collectionName(String name) {
        return environment.getProperty("management.mongodb.prefix", "") + name;
    }

    private MongoDatabase database() {
        return mongoClient.getDatabase(subscriptions().getNamespace().getDatabaseName());
    }

    private static String hashOf(Subscription subscription) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(SubscriptionUniqueActiveKey.of(subscription).getBytes(UTF_8))
        );
    }

    private static Subscription aSubscription(String id, Subscription.Status status) {
        return aSubscription(id, status, "plan-1");
    }

    private static Subscription aSubscription(String id, Subscription.Status status, String plan) {
        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEnvironmentId("DEFAULT");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api-1");
        subscription.setApi("api-1");
        subscription.setApplication("app-1");
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setCreatedAt(new Date());
        subscription.setUpdatedAt(new Date());
        return subscription;
    }
}
