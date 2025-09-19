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

import static io.gravitee.repository.management.model.Subscription.Status.*;

import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Subscription;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class SubscriptionFetcher {

    private static final List<String> STATUSES = List.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name());
    private final SubscriptionRepository subscriptionRepository;

    public Flowable<List<Subscription>> fetchLatest(final Long from, final Long to, final Set<String> environments) {
        // The following doesn't paginate over the result because for now we don't see any value, but it could be implemented same as EventFetcher
        return Flowable.generate(emitter -> {
            SubscriptionCriteria criteriaBuilder = SubscriptionCriteria.builder()
                .statuses(STATUSES)
                .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                .environments(environments)
                .build();
            try {
                List<Subscription> subscriptions = subscriptionRepository.search(
                    criteriaBuilder,
                    new SortableBuilder().field("updatedAt").order(Order.ASC).build()
                );
                if (subscriptions != null && !subscriptions.isEmpty()) {
                    emitter.onNext(subscriptions);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
