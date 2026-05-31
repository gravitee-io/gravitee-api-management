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
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

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
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().plans(singleton("plan1")).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(1, subscriptions.size(), "Subscriptions size");
        final Subscription subscription = subscriptions.getFirst();
        assertEquals("sub1", subscription.getId(), "Subscription id");
        assertEquals("plan1", subscription.getPlan(), "Subscription plan");
        assertEquals("app1", subscription.getApplication(), "Subscription application");
        assertEquals("app1 name", subscription.getApplicationName(), "Subscription application name");
        assertEquals("api1", subscription.getApi(), "Subscription api");
        assertEquals("reason", subscription.getReason(), "Subscription reason");
        assertEquals("request", subscription.getRequest(), "Subscription request");
        assertEquals(PENDING, subscription.getStatus(), "Subscription status");
        assertEquals(Subscription.ConsumerStatus.STARTED, subscription.getConsumerStatus(), "Subscription consumer status");
        assertEquals("user1", subscription.getProcessedBy(), "Subscription processed by");
        assertEquals("user2", subscription.getSubscribedBy(), "Subscription subscribed by");
        assertTrue(compareDate(1439022010883L, subscription.getStartingAt().getTime()), "Subscription starting at");
        assertTrue(compareDate(1449022010883L, subscription.getEndingAt().getTime()), "Subscription ending at");
        assertTrue(compareDate(1459022010883L, subscription.getCreatedAt().getTime()), "Subscription created at");
        assertTrue(compareDate(1469022010883L, subscription.getUpdatedAt().getTime()), "Subscription updated at");
        assertTrue(compareDate(1479022010883L, subscription.getProcessedAt().getTime()), "Subscription processed at");
        assertTrue(compareDate(1479022010883L, subscription.getPausedAt().getTime()), "Subscription paused at");
        assertTrue(compareDate(1479022010883L, subscription.getConsumerPausedAt().getTime()), "Subscription consumer paused at");
        assertEquals("my-client-id", subscription.getClientId(), "Subscription client id");
        assertEquals("my-client-certificate", subscription.getClientCertificate(), "Subscription client certficate");
        assertTrue(subscription.getGeneralConditionsAccepted(), "Subscription GCU accepted");
        assertEquals("ref", subscription.getGeneralConditionsContentPageId(), "Subscription GCU content pageId");
        assertEquals(Integer.valueOf(2), subscription.getGeneralConditionsContentRevision(), "Subscription GCU content revision");
        assertTrue(subscription.getMetadata().containsKey("key"), "Subscription metadata");
        assertEquals("value", subscription.getMetadata().get("key"), "Subscription metadata");
        assertEquals("{}", subscription.getConfiguration(), "Subscription configuration");
        assertEquals(Subscription.Type.STANDARD, subscription.getType(), "Subscription type");
        assertEquals(
            Integer.valueOf(30),
            subscription.getDaysToExpirationOnLastNotification(),
            "Subscription days to expiration on last notification"
        );
    }

    @Test
    public void shouldNotFindByPlan() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().plans(singleton("unknown-plan")).build()
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().applications(singleton("app1")).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(3, subscriptions.size(), "Subscriptions size");
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("sub3", iterator.next().getId(), "Subscription id");
        assertEquals("sub4", iterator.next().getId(), "Subscription id");
        assertEquals("sub1", iterator.next().getId(), "Subscription id");
    }

    @Test
    public void shoulNotFindByApplication() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().applications(singleton("unknown-app")).build()
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByEnvironment() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().environments(singleton("DEFAULT")).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(3, subscriptions.size(), "Subscriptions size");
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("sub3", iterator.next().getId(), "Subscription id");
        assertEquals("sub2", iterator.next().getId(), "Subscription id");
        assertEquals("sub1", iterator.next().getId(), "Subscription id");
    }

    @Test
    public void shoulNotFindByEnvironment() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().applications(singleton("unknown-env")).build()
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().ids(List.of("sub1", "sub3", "sub4")).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(3, subscriptions.size(), "Subscriptions size");
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("sub3", iterator.next().getId(), "Subscription id");
        assertEquals("sub4", iterator.next().getId(), "Subscription id");
        assertEquals("sub1", iterator.next().getId(), "Subscription id");
    }

    @Test
    public void shoulNotFindByIds() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().ids(singleton("unknown-subscription")).build()
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Subscription> optionalSubscription = this.subscriptionRepository.findById("sub1");

        assertNotNull(optionalSubscription);
        assertTrue(optionalSubscription.isPresent());
        final Subscription subscription = optionalSubscription.get();
        assertEquals("sub1", subscription.getId(), "Subscription id");
        assertEquals("plan1", subscription.getPlan(), "Subscription plan");
        assertEquals("app1", subscription.getApplication(), "Subscription application");
        assertEquals("api1", subscription.getApi(), "Subscription api");
        assertEquals("reason", subscription.getReason(), "Subscription reason");
        assertEquals(PENDING, subscription.getStatus(), "Subscription status");
        assertEquals("user1", subscription.getProcessedBy(), "Subscription processed by");
        assertEquals("user2", subscription.getSubscribedBy(), "Subscription subscribed by");
        assertTrue(compareDate(1439022010883L, subscription.getStartingAt().getTime()), "Subscription starting at");
        assertTrue(compareDate(1449022010883L, subscription.getEndingAt().getTime()), "Subscription ending at");
        assertTrue(compareDate(1459022010883L, subscription.getCreatedAt().getTime()), "Subscription created at");
        assertTrue(compareDate(1469022010883L, subscription.getUpdatedAt().getTime()), "Subscription updated at");
        assertTrue(compareDate(1479022010883L, subscription.getProcessedAt().getTime()), "Subscription processed at");
        assertEquals("my-client-id", subscription.getClientId(), "Subscription client id");
        assertEquals("my-client-certificate", subscription.getClientCertificate(), "Subscription client certificate");
    }

    @Test
    public void shouldFindByIdIn_withUnknownId() throws TechnicalException {
        List<Subscription> subscriptions = subscriptionRepository.findByIdIn(Set.of("sub1", "unknown-id"));
        assertEquals(1, subscriptions.size());
        Subscription subscription = subscriptions.getFirst();
        assertEquals("sub1", subscription.getId(), "Subscription id");
        assertEquals("plan1", subscription.getPlan(), "Subscription plan");
        assertEquals("app1", subscription.getApplication(), "Subscription application");
        assertEquals("api1", subscription.getApi(), "Subscription api");
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
        var subscription = this.subscriptionRepository.findById("sub1").orElseThrow();
        subscription.setUpdatedAt(new Date(1000000000000L));

        Subscription update = this.subscriptionRepository.update(subscription);

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

    @Test
    public void shouldNotUpdateUnknownSubscription() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Subscription unknownSubscription = new Subscription();
            unknownSubscription.setId("unknown");
            subscriptionRepository.update(unknownSubscription);
            fail("An unknown subscription should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            subscriptionRepository.update(null);
            fail("A null subscription should not be updated");
        });
    }

    @Test
    public void shouldFindBetweenDates() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().from(1469022010883L).to(1569022010883L).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(1, subscriptions.size(), "Subscriptions size");
        assertEquals("sub1", subscriptions.getFirst().getId(), "Subscription id");
    }

    @Test
    public void shouldFindAfterFromDate() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(SubscriptionCriteria.builder().from(1469022010883L).build());

        assertEquals(4, subscriptions.size(), "Subscriptions size");
        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(subscriptionIds.contains("sub3"), "Should contain sub3");
        assertTrue(subscriptionIds.contains("sub1"), "Should contain sub1");
        assertTrue(subscriptionIds.contains("sub-api-product-1"), "Should contain sub-api-product-1");
        assertTrue(subscriptionIds.contains("sub-api-product-2"), "Should contain sub-api-product-2");
    }

    @Test
    public void shouldFindBeforeToDate() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(SubscriptionCriteria.builder().to(1569022010883L).build());

        assertEquals(13, subscriptions.size(), "Subscriptions size");
        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(subscriptionIds.contains("sub1"));
        assertTrue(
            subscriptionIds.containsAll(
                List.of(
                    "sub-sa-1",
                    "sub-sa-2a",
                    "sub-sa-2b",
                    "sub-sa-3-a",
                    "sub-sa-3-b",
                    "sub-sa-3-c",
                    "sub-sa-3-d",
                    "sub-sa-3-e",
                    "sub-sa-4-a",
                    "sub-sa-4-b",
                    "sub-sa-4-c",
                    "sub-sa-4-d"
                )
            )
        );
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

        subscriptionPage = subscriptionRepository.search(
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
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().endingAtAfter(1449022010880L).endingAtBefore(1569022010883L).build()
        );

        assertEquals(1, subscriptions.size(), "Subscriptions size");
        assertEquals("sub1", subscriptions.getFirst().getId(), "Subscription id");
    }

    @Test
    public void shouldFindAfterEndingAtDate() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().endingAtAfter(1449022010880L).build()
        );

        assertEquals(1, subscriptions.size(), "Subscriptions size");
        assertEquals("sub1", subscriptions.getFirst().getId(), "Subscription id");
    }

    @Test
    public void shouldFindAfterEndingAtDateWithoutEndingAt() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().endingAtAfter(1449022010880L).includeWithoutEnd(true).build()
        );

        assertEquals(21, subscriptions.size(), "Subscriptions size");
        assertTrue(
            List.of(
                "sub3",
                "sub2",
                "sub5",
                "sub4",
                "sub1",
                "sub6",
                "sub7",
                "sub8",
                "sub-legacy-push",
                "sub-sa-1",
                "sub-sa-2a",
                "sub-sa-2b",
                "sub-sa-3-a",
                "sub-sa-3-b",
                "sub-sa-3-c",
                "sub-sa-3-d",
                "sub-sa-3-e",
                "sub-sa-4-a",
                "sub-sa-4-b",
                "sub-sa-4-c",
                "sub-sa-4-d"
            ).containsAll(subscriptions.stream().map(Subscription::getId).toList()),
            "Subscription id"
        );
    }

    @Test
    public void shouldFindBeforeEndingAtDate() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().endingAtBefore(1569022010883L).build()
        );

        assertEquals(3, subscriptions.size(), "Subscriptions size");
        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(subscriptionIds.contains("sub1"), "Should contain sub1");
        assertTrue(subscriptionIds.contains("sub-api-product-1"), "Should contain sub-api-product-1");
        assertTrue(subscriptionIds.contains("sub-api-product-2"), "Should contain sub-api-product-2");
    }

    @Test
    public void shouldFindBeforeEndingAtDateIncludingWithoutEndingAt() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().endingAtBefore(1569022010883L).includeWithoutEnd(true).build()
        );

        assertEquals(23, subscriptions.size(), "Subscriptions size");
        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(
            subscriptionIds.containsAll(
                List.of(
                    "sub3",
                    "sub2",
                    "sub5",
                    "sub4",
                    "sub1",
                    "sub6",
                    "sub7",
                    "sub8",
                    "sub-api-product-1",
                    "sub-api-product-2",
                    "sub-legacy-push"
                )
            ),
            "Should contain expected subscriptions"
        );
    }

    @Test
    public void shouldFindReferenceIdsOrderByNumberOfSubscriptionsDesc() throws TechnicalException {
        Set<String> ranking = this.subscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(
            SubscriptionCriteria.builder().statuses(List.of(PENDING.name())).build(),
            Order.DESC
        );

        assertEquals(1, ranking.size(), "Ranking size");
        assertEquals("api1", ranking.iterator().next(), "Ranking");
    }

    @Test
    public void shouldComputeRankingByReferenceTypeAndIds() throws TechnicalException {
        Set<String> ranking = this.subscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(
            SubscriptionCriteria.builder()
                .referenceType(SubscriptionReferenceType.API)
                .referenceIds(List.of("api1"))
                .statuses(List.of(PENDING.name()))
                .build(),
            Order.DESC
        );

        assertNotNull(ranking);
        assertEquals(1, ranking.size(), "Ranking size");
        assertEquals("api1", ranking.iterator().next(), "Ranking");
    }

    @Test
    public void should_search_subscriptions_including_legacy_api_field() throws TechnicalException {
        // Regression test for APIM-13150: subscriptions with api field set but no referenceId/referenceType
        // (created by old nodes during rolling upgrade from 4.10 to 4.11) must be included in results
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().referenceIds(List.of("api1")).referenceType(SubscriptionReferenceType.API).build()
        );

        assertNotNull(subscriptions);
        Set<String> ids = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("sub1"), "Should include migrated subscription (referenceId/referenceType set)");
        assertTrue(ids.contains("sub-legacy-push"), "Should include legacy subscription with only api field set");
    }

    @Test
    public void shouldComputeRankingByApplications() throws TechnicalException {
        Set<String> ranking = this.subscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(
            SubscriptionCriteria.builder().applications(Arrays.asList("app1", "app2")).build(),
            Order.DESC
        );

        assertEquals(2, ranking.size(), "Ranking size");
        Iterator<String> iterator = ranking.iterator();
        assertEquals("app1", iterator.next(), "First");
        assertEquals("app2", iterator.next(), "Second");
    }

    @Test
    public void shouldFindBySecurityType() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().planSecurityTypes(List.of(API_KEY.name(), OAUTH2.name())).build()
        );

        assertNotNull(subscriptions);
        assertEquals(5, subscriptions.size());
        // Results may be in different order, so check that all expected subscriptions are present
        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(java.util.stream.Collectors.toSet());
        assertTrue(subscriptionIds.contains("sub5"), "Should contain sub5");
        assertTrue(subscriptionIds.contains("sub4"), "Should contain sub4");
        assertTrue(subscriptionIds.contains("sub1"), "Should contain sub1");
        assertTrue(subscriptionIds.contains("sub-api-product-1"), "Should contain sub-api-product-1");
        assertTrue(subscriptionIds.contains("sub-api-product-2"), "Should contain sub-api-product-2");
    }

    @Test
    public void shouldFindBySecurityTypeAndStatus() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder()
                .planSecurityTypes(List.of(API_KEY.name(), OAUTH2.name()))
                .statuses(List.of(PENDING.name()))
                .build()
        );

        assertNotNull(subscriptions);
        assertEquals(3, subscriptions.size());
        // Results may be in different order, so check that all expected subscriptions are present
        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(java.util.stream.Collectors.toSet());
        assertTrue(subscriptionIds.contains("sub1"), "Should contain sub1");
        assertTrue(subscriptionIds.contains("sub4"), "Should contain sub4");
        assertTrue(subscriptionIds.contains("sub-api-product-2"), "Should contain sub-api-product-2");
    }

    @Test
    public void shouldSearchByIdsSortedById() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().ids(List.of("sub4", "sub3", "sub1")).build(),
            new SortableBuilder().order(Order.ASC).field("id").build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(3, subscriptions.size(), "Subscriptions size");
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("sub1", iterator.next().getId(), "Subscription id");
        assertEquals("sub3", iterator.next().getId(), "Subscription id");
        assertEquals("sub4", iterator.next().getId(), "Subscription id");
    }

    @Test
    public void shouldSearchByIdsSortedByCreated() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().ids(List.of("sub4", "sub3", "sub1")).build(),
            new SortableBuilder().order(Order.ASC).field("createdAt").build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(3, subscriptions.size(), "Subscriptions size");
        final Iterator<Subscription> iterator = subscriptions.iterator();
        assertEquals("sub1", iterator.next().getId(), "Subscription id");
        assertEquals("sub4", iterator.next().getId(), "Subscription id");
        assertEquals("sub3", iterator.next().getId(), "Subscription id");
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
            subscriptionRepository
                .findAll()
                .stream()
                .filter(apiKey -> "ToBeDeleted".equals(apiKey.getEnvironmentId()))
                .count()
        );
    }

    @Test
    public void shouldSearchByReferenceTypeOnlyApi() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().referenceType(SubscriptionReferenceType.API).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        subscriptions.forEach(sub ->
            assertTrue(
                sub.getReferenceType() == null || sub.getReferenceType() == SubscriptionReferenceType.API,
                "Each result must be API or legacy (null) referenceType"
            )
        );
        Set<String> ids = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("sub1"), "Should include API subscription sub1");
    }

    @Test
    public void shouldSearchByReferenceTypeOnlyApiProduct() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().referenceType(SubscriptionReferenceType.API_PRODUCT).build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        subscriptions.forEach(sub -> assertEquals(SubscriptionReferenceType.API_PRODUCT, sub.getReferenceType()));
        Set<String> ids = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("sub-api-product-1"));
        assertTrue(ids.contains("sub-api-product-2"));
    }

    @Test
    public void shouldSearchByReferenceIdsAndType() throws TechnicalException {
        // Criteria with referenceIds/referenceType for API subscriptions
        // Expects both the migrated subscription (referenceId/referenceType set) and the legacy one (only api field set)
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().referenceIds(List.of("api1")).referenceType(SubscriptionReferenceType.API).build()
        );

        assertNotNull(subscriptions);
        Set<String> ids = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("sub1"), "Should include migrated subscription");
        assertTrue(ids.contains("sub-legacy-push"), "Should include legacy subscription (only api field set)");
    }

    @Test
    public void should_search_by_multiple_reference_ids_including_legacy_api_field() throws TechnicalException {
        // Regression test for APIM-13150: the multi-ID (IN clause) path must also include legacy subscriptions
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder().referenceIds(List.of("api1", "api-unknown")).referenceType(SubscriptionReferenceType.API).build()
        );

        assertNotNull(subscriptions);
        Set<String> ids = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("sub1"), "Should include migrated subscription");
        assertTrue(ids.contains("sub-legacy-push"), "Should include legacy subscription with only api field set");
    }

    @Test
    public void shouldSearchByReferenceIdsAndReferenceType() throws TechnicalException {
        List<Subscription> subscriptions = this.subscriptionRepository.search(
            SubscriptionCriteria.builder()
                .referenceIds(List.of("c45b8e66-4d2a-47ad-9b8e-664d2a97ad88"))
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .build()
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(2, subscriptions.size(), "Subscriptions size");

        Set<String> subscriptionIds = subscriptions.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertTrue(subscriptionIds.contains("sub-api-product-1"), "Should contain sub-api-product-1");
        assertTrue(subscriptionIds.contains("sub-api-product-2"), "Should contain sub-api-product-2");

        subscriptions.forEach(subscription -> {
            assertEquals("c45b8e66-4d2a-47ad-9b8e-664d2a97ad88", subscription.getReferenceId(), "API Product ID");
            assertEquals(SubscriptionReferenceType.API_PRODUCT, subscription.getReferenceType(), "Reference Type");
        });
    }

    @Test
    public void shouldFindByReferenceIdAndReferenceType() throws TechnicalException {
        Set<Subscription> subscriptions = subscriptionRepository.findByReferenceIdAndReferenceType(
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscriptions);
        assertFalse(subscriptions.isEmpty());
        assertEquals(2, subscriptions.size(), "Subscriptions size");

        // Verify all subscriptions belong to the api-product
        subscriptions.forEach(subscription -> {
            assertEquals("c45b8e66-4d2a-47ad-9b8e-664d2a97ad88", subscription.getReferenceId(), "API Product ID");
            assertEquals(SubscriptionReferenceType.API_PRODUCT, subscription.getReferenceType(), "Reference Type");
        });
    }

    @Test
    public void shouldNotFindByReferenceIdAndReferenceTypeWhenNoMatch() throws TechnicalException {
        Set<Subscription> subscriptions = subscriptionRepository.findByReferenceIdAndReferenceType(
            "non-existent-api-product",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscriptions);
        assertTrue(subscriptions.isEmpty());
    }

    @Test
    public void shouldFindByIdAndReferenceIdAndReferenceType() throws TechnicalException {
        Optional<Subscription> subscription = subscriptionRepository.findByIdAndReferenceIdAndReferenceType(
            "sub-api-product-1",
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscription);
        assertTrue(subscription.isPresent());
        assertEquals("sub-api-product-1", subscription.get().getId(), "Subscription id");
        assertEquals("c45b8e66-4d2a-47ad-9b8e-664d2a97ad88", subscription.get().getReferenceId(), "API Product ID");
        assertEquals(SubscriptionReferenceType.API_PRODUCT, subscription.get().getReferenceType(), "Reference Type");
    }

    @Test
    public void shouldNotFindByIdAndReferenceIdAndReferenceTypeWhenWrongReferenceId() throws TechnicalException {
        Optional<Subscription> subscription = subscriptionRepository.findByIdAndReferenceIdAndReferenceType(
            "sub-api-product-1",
            "api-product-2",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscription);
        assertFalse(subscription.isPresent());
    }

    @Test
    public void shouldNotFindByIdAndReferenceIdAndReferenceTypeWhenSubscriptionDoesNotExist() throws TechnicalException {
        Optional<Subscription> subscription = subscriptionRepository.findByIdAndReferenceIdAndReferenceType(
            "non-existent",
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertNotNull(subscription);
        assertFalse(subscription.isPresent());
    }

    @Test
    public void searchUnorderedShouldReturnSameSetAsSearch() throws TechnicalException {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().applications(singleton("app1")).build();

        List<Subscription> sorted = subscriptionRepository.search(criteria);
        List<Subscription> unordered = subscriptionRepository.searchUnordered(criteria);

        assertNotNull(unordered);
        assertEquals(sorted.size(), unordered.size(), "Subscriptions size");
        Set<String> sortedIds = sorted.stream().map(Subscription::getId).collect(Collectors.toSet());
        Set<String> unorderedIds = unordered.stream().map(Subscription::getId).collect(Collectors.toSet());
        assertEquals(sortedIds, unorderedIds, "Subscription id set");
    }

    @Test
    public void searchAfter_shouldReturnSingleSubscriptionForEnvironment() throws TechnicalException {
        List<Subscription> page = subscriptionRepository.searchAfter(
            SubscriptionCriteria.builder().environments(singleton("env-sa-1")).build(),
            new SortableBuilder().field("updatedAt").order(Order.ASC).build(),
            null,
            10
        );

        assertNotNull(page);
        assertEquals(1, page.size());
        assertEquals("sub-sa-1", page.getFirst().getId());
    }

    @Test
    public void searchAfter_shouldPaginateByPlanKeysetWithPlansFilter() throws TechnicalException {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().plans(List.of("plan3")).build();
        // Warmup path: sort field "plan" → (plan, id) keyset, paired with byPlanAndId cursor.
        var sortable = new SortableBuilder().field("plan").order(Order.ASC).build();

        Set<String> collected = new java.util.LinkedHashSet<>();
        SubscriptionCursor cursor = null;
        int pageSize = 2;
        int guard = 10;
        while (guard-- > 0) {
            List<Subscription> page = subscriptionRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (Subscription s : page) {
                assertTrue(collected.add(s.getId()), "Duplicate id across pages: " + s.getId());
            }
            Subscription last = page.getLast();
            cursor = SubscriptionCursor.byPlanAndId(last.getPlan(), last.getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        assertEquals(
            Set.of("sub-legacy-push", "sub2", "sub3", "sub6", "sub7", "sub8"),
            collected,
            "plan3 subs returned exactly — no leak from unrelated plans"
        );
    }

    @Test
    public void searchAfter_shouldPaginateByIdOnlyFallback() throws TechnicalException {
        // Legacy / repository-bridge fallback: sort field "id" → id-only keyset, paired with byId
        // cursor. The seek and the sort must both be id-only — if the sort were (plan, id) here while
        // the seek stays id-only (the bug this guards), keyset pagination would skip or duplicate rows.
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().plans(List.of("plan3")).build();
        var sortable = new SortableBuilder().field("id").order(Order.ASC).build();

        // Ground truth: a single page (no keyset) gives the engine's own id order. Collation differs
        // per engine, so we compare against this rather than a hard-coded order.
        List<String> singlePageOrder = subscriptionRepository
            .searchAfter(criteria, sortable, null, 100)
            .stream()
            .map(Subscription::getId)
            .collect(Collectors.toList());

        // Paginate the same query in small pages via the byId cursor.
        List<String> paged = new java.util.ArrayList<>();
        SubscriptionCursor cursor = null;
        int pageSize = 2;
        int guard = 10;
        while (guard-- > 0) {
            List<Subscription> page = subscriptionRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            page.forEach(s -> paged.add(s.getId()));
            cursor = SubscriptionCursor.byId(page.getLast().getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        // Keyset pagination must reproduce the single-page order exactly — same elements, same order,
        // no skips or duplicates. This holds only when the seek predicate agrees with the sort.
        assertEquals("id-only keyset pagination must reproduce the single-page id order (no skip/dup/reorder)", singlePageOrder, paged);
        assertEquals(
            "plan3 subs returned exactly — no leak from unrelated plans",
            Set.of("sub-legacy-push", "sub2", "sub3", "sub6", "sub7", "sub8"),
            new java.util.HashSet<>(paged)
        );
    }

    @Test
    public void searchAfter_shouldHonourEndingAtAfterWithIncludeWithoutEnd() throws TechnicalException {
        // Warmup criteria — initial sync filter uses endingAtAfter(now) + includeWithoutEnd(true)
        // to load subs that are active OR have no expiry. searchAfter's net-new criteria support
        // must produce the same set as the legacy search() for this exact shape.
        SubscriptionCriteria criteria = SubscriptionCriteria.builder()
            .plans(List.of("plan-sa"))
            .endingAtAfter(2500L)
            .includeWithoutEnd(true)
            .build();
        var sortable = new SortableBuilder().field("plan").order(Order.ASC).build();

        Set<String> viaSearchAfter = new java.util.LinkedHashSet<>();
        SubscriptionCursor cursor = null;
        int pageSize = 50;
        int guard = 20;
        while (guard-- > 0) {
            List<Subscription> page = subscriptionRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            page.forEach(s -> viaSearchAfter.add(s.getId()));
            cursor = SubscriptionCursor.byPlanAndId(page.getLast().getPlan(), page.getLast().getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        Set<String> viaLegacySearch = subscriptionRepository.search(criteria).stream().map(Subscription::getId).collect(Collectors.toSet());

        assertEquals(
            viaLegacySearch,
            viaSearchAfter,
            "searchAfter and legacy search() must return identical set for endingAtAfter+includeWithoutEnd"
        );
    }

    @Test
    public void searchAfter_shouldRejectUnsupportedCriteria() {
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        // planSecurityTypes rejected
        SubscriptionCriteria withPlanSecurity = SubscriptionCriteria.builder().planSecurityTypes(List.of(API_KEY.name())).build();
        try {
            subscriptionRepository.searchAfter(withPlanSecurity, sortable, null, 10);
            fail("Expected TechnicalException for planSecurityTypes");
        } catch (TechnicalException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("planscuritytypes".replace("scur", "secur")));
        }

        // excludedApis rejected
        SubscriptionCriteria withExcludedApis = SubscriptionCriteria.builder().excludedApis(List.of("api-1")).build();
        try {
            subscriptionRepository.searchAfter(withExcludedApis, sortable, null, 10);
            fail("Expected TechnicalException for excludedApis");
        } catch (TechnicalException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("excludedapis"));
        }
    }

    @Test
    public void searchAfter_shouldReturnEmptyWhenNoMatch() throws TechnicalException {
        List<Subscription> page = subscriptionRepository.searchAfter(
            SubscriptionCriteria.builder().environments(singleton("env-sa-empty")).build(),
            new SortableBuilder().field("updatedAt").order(Order.ASC).build(),
            null,
            10
        );

        assertNotNull(page);
        assertTrue(page.isEmpty());
    }

    @Test
    public void searchAfter_shouldTerminateOnExactMultipleOfPageSize() throws TechnicalException {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().environments(singleton("env-sa-4")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();
        int pageSize = 2;

        List<Subscription> page1 = subscriptionRepository.searchAfter(criteria, sortable, null, pageSize);
        assertEquals(2, page1.size());

        Subscription last = page1.getLast();
        List<Subscription> page2 = subscriptionRepository.searchAfter(
            criteria,
            sortable,
            SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            pageSize
        );
        assertEquals(2, page2.size());

        last = page2.getLast();
        List<Subscription> page3 = subscriptionRepository.searchAfter(
            criteria,
            sortable,
            SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            pageSize
        );
        assertTrue(page3.isEmpty(), "Page after exact-multiple total must be empty");
    }

    @Test
    public void searchAfter_shouldNotSkipOrDuplicateAcrossSameMsBoundary() throws TechnicalException {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().environments(singleton("env-sa-3")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        Set<String> collected = new java.util.LinkedHashSet<>();
        SubscriptionCursor cursor = null;
        int pageSize = 2;
        int guard = 10;
        while (guard-- > 0) {
            List<Subscription> page = subscriptionRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (Subscription s : page) {
                assertTrue(collected.add(s.getId()), "Duplicate id across pages: " + s.getId());
            }
            Subscription last = page.getLast();
            cursor = SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        assertEquals(
            Set.of("sub-sa-3-a", "sub-sa-3-b", "sub-sa-3-c", "sub-sa-3-d", "sub-sa-3-e"),
            collected,
            "All 5 same-ms subs returned exactly once"
        );
    }

    @Test
    public void searchAfter_shouldReturnCompleteMetadataAndNotShortPageDueToJoin() throws TechnicalException {
        // Regression: SQL `LIMIT` applied to a JOIN of subscriptions + metadata would consume
        // metadata rows against the page budget. With pageSize=2 and two subs (3 + 2 metadata rows),
        // a naive LIMIT 2 on the join returns only the first sub with partial metadata. The page
        // must contain exactly the two subscriptions with their complete metadata maps.
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().environments(singleton("env-sa-2")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        List<Subscription> page = subscriptionRepository.searchAfter(criteria, sortable, null, 2);
        assertEquals(2, page.size());
        Subscription a = page
            .stream()
            .filter(s -> "sub-sa-2a".equals(s.getId()))
            .findFirst()
            .orElseThrow();
        Subscription b = page
            .stream()
            .filter(s -> "sub-sa-2b".equals(s.getId()))
            .findFirst()
            .orElseThrow();
        assertEquals(java.util.Map.of("k1", "v1", "k2", "v2", "k3", "v3"), a.getMetadata());
        assertEquals(java.util.Map.of("kA", "vA", "kB", "vB"), b.getMetadata());

        // Short page contract: next call past last sub returns empty
        Subscription last = page.getLast();
        List<Subscription> next = subscriptionRepository.searchAfter(
            criteria,
            sortable,
            SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            2
        );
        assertTrue(next.isEmpty());
    }

    @Test
    public void searchAfter_shouldAdvanceSubscriptionCursorAcrossPagesAndTerminate() throws TechnicalException {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder().environments(singleton("env-sa-2")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        List<Subscription> page1 = subscriptionRepository.searchAfter(criteria, sortable, null, 1);
        assertEquals(1, page1.size());
        assertEquals("sub-sa-2a", page1.getFirst().getId());

        Subscription last = page1.getFirst();
        List<Subscription> page2 = subscriptionRepository.searchAfter(
            criteria,
            sortable,
            SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            1
        );
        assertEquals(1, page2.size());
        assertEquals("sub-sa-2b", page2.getFirst().getId());

        last = page2.getFirst();
        List<Subscription> page3 = subscriptionRepository.searchAfter(
            criteria,
            sortable,
            SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            1
        );
        assertTrue(page3.isEmpty());
    }
}
