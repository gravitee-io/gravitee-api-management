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
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;
import static io.gravitee.repository.management.model.Subscription.Status.CLOSED;
import static io.gravitee.repository.management.model.Subscription.Status.PAUSED;
import static io.gravitee.repository.management.model.Subscription.Status.PENDING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.model.Subscription;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFetcherTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionFetcher cut;

    private static final int BULK_ITEMS = 3;

    @BeforeEach
    public void beforeEach() {
        cut = new SubscriptionFetcher(subscriptionRepository, BULK_ITEMS);
    }

    @Test
    void should_fetch_subscriptions() throws TechnicalException {
        Subscription subscription = new Subscription();
        subscription.setId("s");
        subscription.setUpdatedAt(new Date(1));
        when(subscriptionRepository.searchAfter(any(), any(), isNull(), eq(BULK_ITEMS))).thenReturn(List.of(subscription));
        cut
            .fetchLatest(null, null, Set.of())
            .test()
            .assertValueCount(1)
            .assertValue(subscriptions -> subscriptions.contains(subscription));
    }

    @Test
    void should_fetch_subscriptions_with_criteria() throws TechnicalException {
        Instant to = Instant.now();
        Instant from = to.minus(1000, ChronoUnit.MILLIS);
        Subscription subscription = new Subscription();
        subscription.setId("s");
        subscription.setUpdatedAt(new Date(from.toEpochMilli()));
        when(
            subscriptionRepository.searchAfter(
                argThat(
                    argument ->
                        argument.getEnvironments().contains("env") &&
                        argument.getStatuses().containsAll(Set.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name())) &&
                        argument.getFrom() < from.toEpochMilli() &&
                        argument.getTo() > to.toEpochMilli()
                ),
                argThat(argument -> argument.field().equals("updatedAt") && argument.order().equals(Order.ASC)),
                isNull(),
                eq(BULK_ITEMS)
            )
        ).thenReturn(List.of(subscription));
        cut
            .fetchLatest(from.toEpochMilli(), to.toEpochMilli(), Set.of("env"))
            .test()
            .assertValueCount(1)
            .assertValue(subscriptions -> subscriptions.contains(subscription));
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() throws TechnicalException {
        when(subscriptionRepository.searchAfter(any(), any(), any(), eq(BULK_ITEMS))).thenThrow(new RuntimeException());
        cut.fetchLatest(-1L, -1L, Set.of()).test().assertError(RuntimeException.class);
    }

    @Test
    void should_emit_one_page_per_full_bulk_then_complete_on_short_page() throws TechnicalException {
        List<Subscription> page1 = bulkSubs("p1-", BULK_ITEMS, 100);
        List<Subscription> page2 = bulkSubs("p2-", BULK_ITEMS, 200);
        List<Subscription> shortPage = bulkSubs("p3-", BULK_ITEMS - 1, 300);

        Subscription last1 = page1.get(page1.size() - 1);
        Subscription last2 = page2.get(page2.size() - 1);

        when(subscriptionRepository.searchAfter(any(), any(), isNull(), eq(BULK_ITEMS))).thenReturn(page1);
        when(
            subscriptionRepository.searchAfter(
                any(),
                any(),
                argThat(cursor -> cursor != null && cursor.id().equals(last1.getId())),
                eq(BULK_ITEMS)
            )
        ).thenReturn(page2);
        when(
            subscriptionRepository.searchAfter(
                any(),
                any(),
                argThat(cursor -> cursor != null && cursor.id().equals(last2.getId())),
                eq(BULK_ITEMS)
            )
        ).thenReturn(shortPage);

        cut.fetchLatest(0L, 1L, Set.of("env")).test().assertValueCount(3).assertComplete();
    }

    @Test
    void should_terminate_on_null_updatedAt_without_npe_wedge() throws TechnicalException {
        // A row with null updatedAt would NPE the cursor advance and wedge every subsequent tick.
        // Guard must end pagination cleanly so the loop survives a single bad row.
        Subscription good = new Subscription();
        good.setId("good");
        good.setUpdatedAt(new Date(100));
        Subscription poisoned = new Subscription();
        poisoned.setId("poisoned");
        poisoned.setUpdatedAt(null);

        when(subscriptionRepository.searchAfter(any(), any(), isNull(), eq(BULK_ITEMS))).thenReturn(List.of(good, poisoned));

        cut.fetchLatest(0L, 1L, Set.of("env")).test().assertValueCount(1).assertComplete().assertNoErrors();
    }

    private static List<Subscription> bulkSubs(String idPrefix, int count, long baseMs) {
        List<Subscription> subs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Subscription s = new Subscription();
            s.setId(idPrefix + i);
            s.setUpdatedAt(new Date(baseMs + i));
            subs.add(s);
        }
        return subs;
    }
}
