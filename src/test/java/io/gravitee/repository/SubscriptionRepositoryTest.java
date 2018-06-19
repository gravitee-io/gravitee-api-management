/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Subscription;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/subscription-tests/";
    }

    @Test
    public void shouldFindByPlan() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .plans(singleton("plan1"))
                .build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 1, subscriptions.size());
        final Subscription subscription = subscriptions.iterator().next();
        assertEquals("Subscription id", "sub1", subscription.getId());
        assertEquals("Subscription plan", "plan1", subscription.getPlan());
        assertEquals("Subscription application", "app1", subscription.getApplication());
        assertEquals("Subscription api", "api1", subscription.getApi());
        assertEquals("Subscription reason", "reason", subscription.getReason());
        assertEquals("Subscription request", "request", subscription.getRequest());
        assertEquals("Subscription status", Subscription.Status.PENDING, subscription.getStatus());
        assertEquals("Subscription processed by", "user1", subscription.getProcessedBy());
        assertEquals("Subscription subscribed by", "user2", subscription.getSubscribedBy());
        assertEquals("Subscription starting at", 1439022010883L, subscription.getStartingAt().getTime());
        assertEquals("Subscription ending at", 1449022010883L, subscription.getEndingAt().getTime());
        assertEquals("Subscription created at", 1459022010883L, subscription.getCreatedAt().getTime());
        assertEquals("Subscription updated at", 1469022010883L, subscription.getUpdatedAt().getTime());
        assertEquals("Subscription processed at", 1479022010883L, subscription.getProcessedAt().getTime());
        assertEquals("Subscription client id", "my-client-id", subscription.getClientId());
    }

    @Test
    public void shouldNotFindByPlan() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .plans(singleton("unknown-plan"))
                        .build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .applications(singleton("app1"))
                        .build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 3, subscriptions.size());
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("Subscription id", "sub3", iterator.next().getId());
        assertEquals("Subscription id", "sub4", iterator.next().getId());
        assertEquals("Subscription id", "sub1", iterator.next().getId());
    }

    @Test
    public void shoulNotFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .applications(singleton("unknown-app"))
                        .build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Subscription> optionalSubscription = this.subscriptionRepository.findById("sub1");

        assertNotNull(optionalSubscription);
        assertTrue(optionalSubscription.isPresent());
        final Subscription subscription = optionalSubscription.get();
        assertEquals("Subscription id", "sub1", subscription.getId());
        assertEquals("Subscription plan", "plan1", subscription.getPlan());
        assertEquals("Subscription application", "app1", subscription.getApplication());
        assertEquals("Subscription api", "api1", subscription.getApi());
        assertEquals("Subscription reason", "reason", subscription.getReason());
        assertEquals("Subscription status", Subscription.Status.PENDING, subscription.getStatus());
        assertEquals("Subscription processed by", "user1", subscription.getProcessedBy());
        assertEquals("Subscription subscribed by", "user2", subscription.getSubscribedBy());
        assertEquals("Subscription starting at", 1439022010883L, subscription.getStartingAt().getTime());
        assertEquals("Subscription ending at", 1449022010883L, subscription.getEndingAt().getTime());
        assertEquals("Subscription created at", 1459022010883L, subscription.getCreatedAt().getTime());
        assertEquals("Subscription updated at", 1469022010883L, subscription.getUpdatedAt().getTime());
        assertEquals("Subscription processed at", 1479022010883L, subscription.getProcessedAt().getTime());
        assertEquals("Subscription client id", "my-client-id", subscription.getClientId());
    }

    @Test
    public void shouldNotFindById() throws TechnicalException {
        Optional<Subscription> subscription = this.subscriptionRepository.findById("unknown-sub");

        assertNotNull(subscription);
        assertFalse(subscription.isPresent());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Optional<Subscription> subscription = this.subscriptionRepository.findById("sub1");
        subscription.get().setUpdatedAt(new Date(1000000000000L));

        Subscription update = this.subscriptionRepository.update(subscription.get());

        assertNotNull(update);
        assertEquals(update.getUpdatedAt(), new Date(1000000000000L));
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        this.subscriptionRepository.delete("sub2");

        Optional<Subscription> subscription = this.subscriptionRepository.findById("sub2");
        assertNotNull(subscription);
        assertFalse(subscription.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownSubscription() throws Exception {
        Subscription unknownSubscription = new Subscription();
        unknownSubscription.setId("unknown");
        subscriptionRepository.update(unknownSubscription);
        fail("An unknown subscription should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        subscriptionRepository.update(null);
        fail("A null subscription should not be updated");
    }

    @Test
    public void shouldFindBetweenDates() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .from(1469022010883L)
                        .to(1569022010883L)
                        .build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldFindBetweenDatesPageable() throws Exception {
        Page<Subscription> subscriptionPage = subscriptionRepository.search(new SubscriptionCriteria.Builder()
                .from(1339022010883L)
                .to(1839022010883L)
                .build(),
                new PageableBuilder().pageNumber(0).pageSize(2).build());

        assertEquals(0, subscriptionPage.getPageNumber());
        assertEquals(2, subscriptionPage.getPageElements());
        assertEquals(2, subscriptionPage.getTotalElements());

        assertEquals(2, subscriptionPage.getContent().size());
        assertEquals("sub3", subscriptionPage.getContent().get(0).getId());
        assertEquals("sub1", subscriptionPage.getContent().get(1).getId());

        subscriptionPage = subscriptionRepository.search(new SubscriptionCriteria.Builder()
                        .from(1339022010883L)
                        .to(1839022010883L)
                        .build(),
                new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertEquals(1, subscriptionPage.getPageNumber());
        assertEquals(0, subscriptionPage.getPageElements());
        assertEquals(2, subscriptionPage.getTotalElements());

        assertEquals(0, subscriptionPage.getContent().size());
    }
}
