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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * The unique active key is a storage detail of the subscriptions collection, never exposed by the repository model,
 * hence a MongoDB specific test reading the stored documents.
 *
 * @author GraviteeSource Team
 */
public class MongoSubscriptionUniqueActiveKeyTest extends AbstractManagementRepositoryTest {

    private static final String FIELD = "uniqueActiveKey";

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    private Environment environment;

    @Inject
    private ApplicationContext applicationContext;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_store_the_unique_active_key_of_an_active_subscription() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_not_store_any_key_for_a_rejected_or_closed_subscription() throws Exception {
        subscriptionRepository.create(aSubscription("sub-rejected", Subscription.Status.REJECTED));
        subscriptionRepository.create(aSubscription("sub-closed", Subscription.Status.CLOSED));

        // A null value would still be matched by the partial filter of the unique index, the field must be absent.
        assertThat(storedDocument("sub-rejected")).doesNotContainKey(FIELD);
        assertThat(storedDocument("sub-closed")).doesNotContainKey(FIELD);
    }

    @Test
    public void should_remove_the_key_when_the_subscription_is_closed() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));

        subscription.setStatus(Subscription.Status.CLOSED);
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1")).doesNotContainKey(FIELD);
    }

    @Test
    public void should_keep_the_key_while_the_subscription_is_paused() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        var key = storedDocument("sub-1").getString(FIELD);

        subscription.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isEqualTo(key);
    }

    @Test
    public void should_recompute_the_key_when_the_subscription_is_transferred_to_another_plan() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));

        subscription.setPlan("plan-2");
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isEqualTo(SubscriptionUniqueActiveKey.of(subscription)).contains("plan-2");
    }

    @Test
    public void should_give_the_key_back_when_a_closed_subscription_is_restored() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.CLOSED));

        subscription.setStatus(Subscription.Status.PENDING);
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_recompute_the_key_when_the_channel_of_a_push_subscription_changes() throws Exception {
        var subscription = aSubscription("sub-1", Subscription.Status.ACCEPTED);
        subscription.setType(Subscription.Type.PUSH);
        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"orders\"}");
        subscription = subscriptionRepository.create(subscription);
        var keyOnOrders = storedDocument("sub-1").getString(FIELD);

        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"invoices\"}");
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD))
            .isNotEqualTo(keyOnOrders)
            .isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    /**
     * A repair scans every subscription at startup, before the node serves requests, while nothing reads the key until
     * the unique index exists.
     */
    @Test
    public void should_not_repair_the_keys_of_the_existing_subscriptions_at_startup() {
        assertThat(applicationContext.getBeansOfType(Upgrader.class).values())
            .extracting(upgrader -> upgrader.getClass().getSimpleName())
            .doesNotContain("SubscriptionUniqueActiveKeyUpgrader");
    }

    @Test
    public void should_write_the_key_of_a_subscription_stored_without_it_the_next_time_it_is_saved() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED));
        // As written by a node of a previous version.
        subscriptions().updateOne(Filters.eq("_id", "sub-1"), new Document("$unset", new Document(FIELD, "")));
        var subscription = subscriptionRepository.findById("sub-1").orElseThrow();

        subscription.setUpdatedAt(new Date());
        subscriptionRepository.update(subscription);

        assertThat(storedDocument("sub-1").getString(FIELD)).isNotNull().isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    /**
     * The repository bridge serialises the repository model for the gateways, the key must stay out of it.
     */
    @Test
    public void should_keep_the_key_out_of_the_repository_model() {
        assertThat(Subscription.class.getDeclaredFields()).noneMatch(field -> field.getName().equalsIgnoreCase(FIELD));
    }

    private Document storedDocument(String id) {
        return subscriptions().find(Filters.eq("_id", id)).first();
    }

    private MongoCollection<Document> subscriptions() {
        return mongoOperations.getCollection(environment.getProperty("management.mongodb.prefix", "") + "subscriptions");
    }

    private static Subscription aSubscription(String id, Subscription.Status status) {
        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEnvironmentId("DEFAULT");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api-1");
        subscription.setApi("api-1");
        subscription.setApplication("app-1");
        subscription.setPlan("plan-1");
        subscription.setStatus(status);
        subscription.setCreatedAt(new Date());
        subscription.setUpdatedAt(new Date());
        return subscription;
    }
}
