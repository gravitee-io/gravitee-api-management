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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * An application has at most one active subscription (neither rejected nor closed) to a plan, or to a channel of a
 * PUSH plan, whatever the number of requests or Management API nodes creating it at the same time.
 *
 * @author GraviteeSource Team
 */
public class SubscriptionUniqueActiveRepositoryTest extends AbstractManagementRepositoryTest {

    private static final String CHANNEL_A = "{\"entrypointId\":\"webhook\",\"channel\":\"channel-a\"}";
    private static final String CHANNEL_B = "{\"entrypointId\":\"webhook\",\"channel\":\"channel-b\"}";

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_refuse_a_second_active_subscription_of_an_application_to_a_plan() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        assertThatThrownBy(() -> subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.PENDING, "plan-1"))).isInstanceOf(
            DuplicateKeyException.class
        );
        assertThat(subscriptionRepository.findById("sub-2")).isEmpty();
    }

    @Test
    public void should_refuse_it_while_the_first_subscription_is_pending_or_paused() throws Exception {
        subscriptionRepository.create(aSubscription("sub-pending", Subscription.Status.PENDING, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-paused", Subscription.Status.PAUSED, "plan-2"));

        assertThatThrownBy(() ->
            subscriptionRepository.create(aSubscription("sub-3", Subscription.Status.ACCEPTED, "plan-1"))
        ).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() ->
            subscriptionRepository.create(aSubscription("sub-4", Subscription.Status.ACCEPTED, "plan-2"))
        ).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_accept_active_subscriptions_of_other_applications_and_to_other_plans() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var otherApplication = aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-1");
        otherApplication.setApplication("app-2");

        subscriptionRepository.create(otherApplication);
        subscriptionRepository.create(aSubscription("sub-3", Subscription.Status.ACCEPTED, "plan-2"));

        assertThat(subscriptionRepository.findByIdIn(List.of("sub-1", "sub-2", "sub-3"))).hasSize(3);
    }

    @Test
    public void should_ignore_rejected_and_closed_subscriptions() throws Exception {
        subscriptionRepository.create(aSubscription("sub-closed-1", Subscription.Status.CLOSED, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-closed-2", Subscription.Status.CLOSED, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-rejected-1", Subscription.Status.REJECTED, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-rejected-2", Subscription.Status.REJECTED, "plan-1"));

        subscriptionRepository.create(aSubscription("sub-active", Subscription.Status.ACCEPTED, "plan-1"));

        assertThat(
            subscriptionRepository.findByIdIn(List.of("sub-closed-1", "sub-closed-2", "sub-rejected-1", "sub-rejected-2", "sub-active"))
        ).hasSize(5);
    }

    @Test
    public void should_accept_a_new_subscription_once_the_previous_one_is_closed() throws Exception {
        var previous = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        previous.setStatus(Subscription.Status.CLOSED);
        subscriptionRepository.update(previous);

        subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-1"));

        assertThat(subscriptionRepository.findById("sub-2")).isPresent();
    }

    @Test
    public void should_accept_a_new_subscription_once_the_previous_one_is_rejected() throws Exception {
        var previous = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.PENDING, "plan-1"));
        previous.setStatus(Subscription.Status.REJECTED);
        subscriptionRepository.update(previous);

        subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.PENDING, "plan-1"));

        assertThat(subscriptionRepository.findById("sub-2")).isPresent();
    }

    @Test
    public void should_keep_pausing_and_resuming_a_subscription() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        subscription.setStatus(Subscription.Status.PAUSED);
        subscription = subscriptionRepository.update(subscription);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription = subscriptionRepository.update(subscription);

        assertThat(subscription.getStatus()).isEqualTo(Subscription.Status.ACCEPTED);
    }

    @Test
    public void should_refuse_to_transfer_a_subscription_to_a_plan_the_application_already_subscribed() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var toTransfer = subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-2"));

        toTransfer.setPlan("plan-1");

        assertThatThrownBy(() -> subscriptionRepository.update(toTransfer)).isInstanceOf(DuplicateKeyException.class);
        assertThat(subscriptionRepository.findById("sub-2")).get().extracting(Subscription::getPlan).isEqualTo("plan-2");
    }

    @Test
    public void should_transfer_a_subscription_to_a_plan_the_application_did_not_subscribe() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var toTransfer = subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-2"));
        toTransfer.setPlan("plan-3");

        subscriptionRepository.update(toTransfer);

        // plan-2 is free again
        subscriptionRepository.create(aSubscription("sub-3", Subscription.Status.ACCEPTED, "plan-2"));
        assertThat(subscriptionRepository.findById("sub-3")).isPresent();
    }

    @Test
    public void should_refuse_to_restore_a_closed_subscription_to_a_plan_subscribed_again() throws Exception {
        var closed = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.CLOSED, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-1"));

        closed.setStatus(Subscription.Status.PENDING);

        assertThatThrownBy(() -> subscriptionRepository.update(closed)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_accept_subscriptions_to_several_channels_of_a_push_plan() throws Exception {
        subscriptionRepository.create(aPushSubscription("sub-1", CHANNEL_A));

        subscriptionRepository.create(aPushSubscription("sub-2", CHANNEL_B));

        assertThat(subscriptionRepository.findByIdIn(List.of("sub-1", "sub-2"))).hasSize(2);
    }

    @Test
    public void should_refuse_a_second_subscription_to_a_channel_of_a_push_plan() throws Exception {
        subscriptionRepository.create(aPushSubscription("sub-1", CHANNEL_A));

        assertThatThrownBy(() -> subscriptionRepository.create(aPushSubscription("sub-2", CHANNEL_A))).isInstanceOf(
            DuplicateKeyException.class
        );
    }

    @Test
    public void should_refuse_to_move_a_push_subscription_to_a_channel_already_subscribed() throws Exception {
        subscriptionRepository.create(aPushSubscription("sub-1", CHANNEL_A));
        var moving = subscriptionRepository.create(aPushSubscription("sub-2", CHANNEL_B));

        moving.setConfiguration(CHANNEL_A);

        assertThatThrownBy(() -> subscriptionRepository.update(moving)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_not_report_a_reused_subscription_id_as_a_duplicate_active_subscription() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        var failure = catchThrowable(() -> subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-2")));

        assertThat(failure).isNotNull().isNotInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_create_only_one_of_identical_subscriptions_created_at_the_same_time() throws Exception {
        int requests = 10;
        CyclicBarrier barrier = new CyclicBarrier(requests);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<Throwable>> results = IntStream.range(0, requests)
                .mapToObj(i ->
                    executor.submit(() -> {
                        barrier.await();
                        return catchThrowable(() ->
                            subscriptionRepository.create(aSubscription("sub-" + i, Subscription.Status.ACCEPTED, "plan-1"))
                        );
                    })
                )
                .toList();

            int created = 0;
            for (Future<Throwable> result : results) {
                Throwable failure = result.get();
                if (failure == null) {
                    created++;
                } else {
                    assertThat(failure).isInstanceOf(DuplicateKeyException.class);
                }
            }
            assertThat(created).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        var active = subscriptionRepository.search(
            SubscriptionCriteria.builder()
                .applications(Set.of("app-1"))
                .plans(Set.of("plan-1"))
                .statuses(Set.of(Subscription.Status.ACCEPTED.name()))
                .build()
        );
        assertThat(active).hasSize(1);
    }

    private static Subscription aPushSubscription(String id, String configuration) {
        var subscription = aSubscription(id, Subscription.Status.ACCEPTED, "push-plan");
        subscription.setType(Subscription.Type.PUSH);
        subscription.setConfiguration(configuration);
        return subscription;
    }

    private static Subscription aSubscription(String id, Subscription.Status status, String plan) {
        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEnvironmentId("DEFAULT");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api-1");
        subscription.setApi("api-1");
        subscription.setApplication("app-1");
        subscription.setApplicationName("app-1 name");
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setCreatedAt(new Date());
        subscription.setUpdatedAt(new Date());
        return subscription;
    }
}
