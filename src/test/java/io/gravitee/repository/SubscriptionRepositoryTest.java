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

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
                        .plans(Collections.singleton("plan1"))
                .build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldNotFindByPlan() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .plans(Collections.singleton("unknown-plan"))
                        .build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .applications(Collections.singleton("app1"))
                        .build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shoulNotFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
                new SubscriptionCriteria.Builder()
                        .applications(Collections.singleton("unknown-app"))
                        .build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Subscription> subscription = this.subscriptionRepository.findById("sub1");

        assertNotNull(subscription);
        assertTrue(subscription.isPresent());
        assertEquals("Subscription id", "sub1", subscription.get().getId());
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
        subscription.get().setUpdatedAt(new Date(0));

        Subscription update = this.subscriptionRepository.update(subscription.get());

        assertNotNull(update);
        assertEquals(update.getUpdatedAt(), new Date(0));
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
}
