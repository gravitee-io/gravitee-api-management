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

import static io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY;
import static io.gravitee.repository.management.model.Plan.PlanSecurityType.OAUTH2;
import static io.gravitee.repository.management.model.Subscription.Status.PENDING;
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Subscription;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/subscription-tests/";
    }

    @Test
    public void shouldFindByPlan() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().plans(singleton("plan1")).build());

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
        assertEquals("Subscription status", PENDING, subscription.getStatus());
        assertEquals("Subscription consumer status", Subscription.ConsumerStatus.STARTED, subscription.getConsumerStatus());
        assertEquals("Subscription processed by", "user1", subscription.getProcessedBy());
        assertEquals("Subscription subscribed by", "user2", subscription.getSubscribedBy());
        assertTrue("Subscription starting at", compareDate(1439022010883L, subscription.getStartingAt().getTime()));
        assertTrue("Subscription ending at", compareDate(1449022010883L, subscription.getEndingAt().getTime()));
        assertTrue("Subscription created at", compareDate(1459022010883L, subscription.getCreatedAt().getTime()));
        assertTrue("Subscription updated at", compareDate(1469022010883L, subscription.getUpdatedAt().getTime()));
        assertTrue("Subscription processed at", compareDate(1479022010883L, subscription.getProcessedAt().getTime()));
        assertTrue("Subscription paused at", compareDate(1479022010883L, subscription.getPausedAt().getTime()));
        assertTrue("Subscription consumer paused at", compareDate(1479022010883L, subscription.getConsumerPausedAt().getTime()));
        assertEquals("Subscription client id", "my-client-id", subscription.getClientId());
        assertEquals("Subscription client certficate", "my-client-certificate", subscription.getClientCertificate());
        assertTrue("Subscription GCU accepted", subscription.getGeneralConditionsAccepted());
        assertEquals("Subscription GCU content pageId", "ref", subscription.getGeneralConditionsContentPageId());
        assertEquals("Subscription GCU content revision", Integer.valueOf(2), subscription.getGeneralConditionsContentRevision());
        assertTrue("Subscription metadata", subscription.getMetadata().containsKey("key"));
        assertEquals("Subscription metadata", "value", subscription.getMetadata().get("key"));
        assertEquals("Subscription configuration", "{}", subscription.getConfiguration());
        assertEquals("Subscription type", Subscription.Type.STANDARD, subscription.getType());
        assertEquals(
            "Subscription days to expiration on last notification",
            Integer.valueOf(30),
            subscription.getDaysToExpirationOnLastNotification()
        );
    }

    @Test
    public void shouldNotFindByPlan() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().plans(singleton("unknown-plan")).build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().applications(singleton("app1")).build());

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
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().applications(singleton("unknown-app")).build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByEnvironment() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().environments(singleton("DEFAULT")).build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 3, subscriptions.size());
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("Subscription id", "sub3", iterator.next().getId());
        assertEquals("Subscription id", "sub2", iterator.next().getId());
        assertEquals("Subscription id", "sub1", iterator.next().getId());
    }

    @Test
    public void shoulNotFindByEnvironment() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().applications(singleton("unknown-env")).build());

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().ids(List.of("sub1", "sub3", "sub4")).build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 3, subscriptions.size());
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("Subscription id", "sub3", iterator.next().getId());
        assertEquals("Subscription id", "sub4", iterator.next().getId());
        assertEquals("Subscription id", "sub1", iterator.next().getId());
    }

    @Test
    public void shoulNotFindByIds() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().ids(singleton("unknown-subscription")).build());

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
        assertEquals("Subscription status", PENDING, subscription.getStatus());
        assertEquals("Subscription processed by", "user1", subscription.getProcessedBy());
        assertEquals("Subscription subscribed by", "user2", subscription.getSubscribedBy());
        assertTrue("Subscription starting at", compareDate(1439022010883L, subscription.getStartingAt().getTime()));
        assertTrue("Subscription ending at", compareDate(1449022010883L, subscription.getEndingAt().getTime()));
        assertTrue("Subscription created at", compareDate(1459022010883L, subscription.getCreatedAt().getTime()));
        assertTrue("Subscription updated at", compareDate(1469022010883L, subscription.getUpdatedAt().getTime()));
        assertTrue("Subscription processed at", compareDate(1479022010883L, subscription.getProcessedAt().getTime()));
        assertEquals("Subscription client id", "my-client-id", subscription.getClientId());
        assertEquals("Subscription client certificate", "my-client-certificate", subscription.getClientCertificate());
    }

    @Test
    public void shouldFindByIdIn_withUnknownId() throws TechnicalException {
        List<Subscription> subscriptions = subscriptionRepository.findByIdIn(Set.of("sub1", "unknown-id"));
        assertEquals(1, subscriptions.size());
        Subscription subscription = subscriptions.iterator().next();
        assertEquals("Subscription id", "sub1", subscription.getId());
        assertEquals("Subscription plan", "plan1", subscription.getPlan());
        assertEquals("Subscription application", "app1", subscription.getApplication());
        assertEquals("Subscription api", "api1", subscription.getApi());
    }

    @Test
    public void shouldFindByIdIn_withEmptyList() throws TechnicalException {
        List<Subscription> subscriptions = subscriptionRepository.findByIdIn(Set.of());
        assertEquals(0, subscriptions.size());
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
        assertTrue(compareDate(update.getUpdatedAt(), new Date(1000000000000L)));
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
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().from(1469022010883L).to(1569022010883L).build());

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldFindAfterFromDate() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(SubscriptionCriteria.builder().from(1469022010883L).build());

        assertEquals("Subscriptions size", 2, subscriptions.size());
        Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("Subscription id", "sub3", iterator.next().getId());
        assertEquals("Subscription id", "sub1", iterator.next().getId());
    }

    @Test
    public void shouldFindBeforeToDate() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(SubscriptionCriteria.builder().to(1569022010883L).build());

        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldFindBetweenDatesPageable() throws Exception {
        Page<Subscription> subscriptionPage = subscriptionRepository.search(
            SubscriptionCriteria.builder().from(1339022010883L).to(1839022010883L).build(),
            null,
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );

        assertEquals(0, subscriptionPage.getPageNumber());
        assertEquals(2, subscriptionPage.getPageElements());
        assertEquals(2, subscriptionPage.getTotalElements());

        assertEquals(2, subscriptionPage.getContent().size());
        assertEquals("sub3", subscriptionPage.getContent().get(0).getId());
        assertEquals("sub1", subscriptionPage.getContent().get(1).getId());

        subscriptionPage =
            subscriptionRepository.search(
                SubscriptionCriteria.builder().from(1339022010883L).to(1839022010883L).build(),
                null,
                new PageableBuilder().pageNumber(1).pageSize(2).build()
            );

        assertEquals(1, subscriptionPage.getPageNumber());
        assertEquals(0, subscriptionPage.getPageElements());
        assertEquals(2, subscriptionPage.getTotalElements());

        assertEquals(0, subscriptionPage.getContent().size());
    }

    @Test
    public void shouldFindBetweenEndingAtDates() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria.builder().endingAtAfter(1449022010880L).endingAtBefore(1569022010883L).build()
                );

        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldFindAfterEndingAtDate() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().endingAtAfter(1449022010880L).build());

        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldFindAfterEndingAtDateWithoutEndingAt() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria.builder().endingAtAfter(1449022010880L).includeWithoutEnd(true).build()
                );

        assertEquals("Subscriptions size", 8, subscriptions.size());
        assertTrue(
            "Subscription id",
            List
                .of("sub3", "sub2", "sub5", "sub4", "sub1", "sub6", "sub7", "sub8")
                .containsAll(subscriptions.stream().map(Subscription::getId).collect(Collectors.toList()))
        );
    }

    @Test
    public void shouldFindBeforeEndingAtDate() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(SubscriptionCriteria.builder().endingAtBefore(1569022010883L).build());

        assertEquals("Subscriptions size", 1, subscriptions.size());
        assertEquals("Subscription id", "sub1", subscriptions.iterator().next().getId());
    }

    @Test
    public void shouldFindBeforeEndingAtDateIncludingWithoutEndingAt() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria.builder().endingAtBefore(1569022010883L).includeWithoutEnd(true).build()
                );

        assertEquals("Subscriptions size", 8, subscriptions.size());
        assertTrue(
            "Subscription id",
            List
                .of("sub3", "sub2", "sub5", "sub4", "sub1", "sub6", "sub7", "sub8")
                .containsAll(subscriptions.stream().map(Subscription::getId).collect(Collectors.toList()))
        );
    }

    @Test
    public void shouldFindReferenceIdsOrderByNumberOfSubscriptionsDesc() throws TechnicalException {
        Set<String> ranking =
            this.subscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(
                    SubscriptionCriteria.builder().statuses(List.of(PENDING.name())).build(),
                    Order.DESC
                );

        assertEquals("Ranking size", 1, ranking.size());
        assertEquals("Ranking", "api1", ranking.iterator().next());
    }

    @Test
    public void shouldComputeRankingByApplications() throws TechnicalException {
        Set<String> ranking =
            this.subscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(
                    SubscriptionCriteria.builder().applications(Arrays.asList("app1", "app2")).build(),
                    Order.DESC
                );

        assertEquals("Ranking size", 2, ranking.size());
        Iterator<String> iterator = ranking.iterator();
        assertEquals("First", "app1", iterator.next());
        assertEquals("Second", "app2", iterator.next());
    }

    @Test
    public void shouldFindBySecurityType() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria.builder().planSecurityTypes(List.of(API_KEY.name(), OAUTH2.name())).build()
                );

        assertNotNull(subscriptions);
        assertEquals(3, subscriptions.size());
        assertEquals("sub5", subscriptions.get(0).getId());
        assertEquals("plan5", subscriptions.get(0).getPlan());
        assertEquals("sub4", subscriptions.get(1).getId());
        assertEquals("plan2", subscriptions.get(1).getPlan());
        assertEquals("sub1", subscriptions.get(2).getId());
        assertEquals("plan1", subscriptions.get(2).getPlan());
    }

    @Test
    public void shouldFindBySecurityTypeAndStatus() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria
                        .builder()
                        .planSecurityTypes(List.of(API_KEY.name(), OAUTH2.name()))
                        .statuses(List.of(PENDING.name()))
                        .build()
                );

        assertNotNull(subscriptions);
        assertEquals(2, subscriptions.size());
        assertEquals("sub4", subscriptions.get(0).getId());
        assertEquals("sub1", subscriptions.get(1).getId());
    }

    @Test
    public void shouldSearchByIdsSortedById() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria.builder().ids(List.of("sub4", "sub3", "sub1")).build(),
                    new SortableBuilder().order(Order.ASC).field("id").build()
                );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 3, subscriptions.size());
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("Subscription id", "sub1", iterator.next().getId());
        assertEquals("Subscription id", "sub3", iterator.next().getId());
        assertEquals("Subscription id", "sub4", iterator.next().getId());
    }

    @Test
    public void shouldSearchByIdsSortedByCreated() throws TechnicalException {
        List<Subscription> subscriptions =
            this.subscriptionRepository.search(
                    SubscriptionCriteria.builder().ids(List.of("sub4", "sub3", "sub1")).build(),
                    new SortableBuilder().order(Order.ASC).field("createdAt").build()
                );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals("Subscriptions size", 3, subscriptions.size());
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("Subscription id", "sub1", iterator.next().getId());
        assertEquals("Subscription id", "sub4", iterator.next().getId());
        assertEquals("Subscription id", "sub3", iterator.next().getId());
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        List<Subscription> subscriptionsBeforeDeletion = subscriptionRepository
            .findAll()
            .stream()
            .filter(apiKey -> "ToBeDeleted".equals(apiKey.getEnvironmentId()))
            .toList();
        assertEquals(3, subscriptionsBeforeDeletion.size());

        List<String> subscriptionsIdsDeleted = subscriptionRepository.deleteByEnvironmentId("ToBeDeleted");

        assertEquals(3, subscriptionsIdsDeleted.size());
        assertEquals(
            0,
            subscriptionRepository.findAll().stream().filter(apiKey -> "ToBeDeleted".equals(apiKey.getEnvironmentId())).count()
        );
    }
}
