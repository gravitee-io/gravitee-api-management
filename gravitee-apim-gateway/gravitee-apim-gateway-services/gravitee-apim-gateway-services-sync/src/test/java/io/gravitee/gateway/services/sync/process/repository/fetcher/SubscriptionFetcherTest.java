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
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.model.Subscription;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @BeforeEach
    public void beforeEach() {
        cut = new SubscriptionFetcher(subscriptionRepository);
    }

    @Test
    void should_fetch_subscriptions() throws TechnicalException {
        Subscription subscription = new Subscription();
        when(subscriptionRepository.search(any(), any())).thenReturn(List.of(subscription));
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
        when(
            subscriptionRepository.search(
                argThat(
                    argument ->
                        argument.getEnvironments().contains("env") &&
                        argument.getStatuses().containsAll(Set.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name())) &&
                        argument.getFrom() < from.toEpochMilli() &&
                        argument.getTo() > to.toEpochMilli()
                ),
                argThat(argument -> argument.field().equals("updatedAt") && argument.order().equals(Order.ASC))
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
        when(subscriptionRepository.search(any(), any())).thenThrow(new RuntimeException());
        cut.fetchLatest(-1L, -1L, Set.of()).test().assertError(RuntimeException.class);
    }
}
