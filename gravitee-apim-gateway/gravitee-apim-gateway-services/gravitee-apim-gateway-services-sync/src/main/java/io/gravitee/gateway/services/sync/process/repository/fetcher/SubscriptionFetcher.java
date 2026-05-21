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
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Subscription;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class SubscriptionFetcher {

    private static final List<String> STATUSES = List.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name());

    private final SubscriptionRepository subscriptionRepository;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    public SubscriptionFetcher(SubscriptionRepository subscriptionRepository, int bulkItems) {
        if (bulkItems <= 0) {
            throw new IllegalArgumentException("bulkItems must be > 0 (got " + bulkItems + ")");
        }
        this.subscriptionRepository = subscriptionRepository;
        this.bulkItems = bulkItems;
    }

    public Flowable<List<Subscription>> fetchLatest(final Long from, final Long to, final Set<String> environments) {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder()
            .statuses(STATUSES)
            .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
            .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
            .environments(environments)
            .build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        return Flowable.<List<Subscription>, SubscriptionPage>generate(
            () -> new SubscriptionPage(null),
            (page, emitter) -> {
                try {
                    List<Subscription> subscriptions = subscriptionRepository.searchAfter(criteria, sortable, page.cursor, bulkItems);
                    if (subscriptions != null && !subscriptions.isEmpty()) {
                        emitter.onNext(subscriptions);
                        Subscription last = subscriptions.getLast();
                        if (last.getUpdatedAt() == null) {
                            // Criteria.from/to > 0 already filters rows lacking updatedAt at the
                            // repository layer. Guard the cursor advance so a malformed row never
                            // wedges the loop (NPE → onError → retry exhaustion → tick never
                            // completes → next tick refetches the same poison row).
                            log.warn("Subscription {} has null updatedAt; terminating page loop early", last.getId());
                            emitter.onComplete();
                            return;
                        }
                        page.cursor = SubscriptionCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId());
                    }
                    if (subscriptions == null || subscriptions.size() < bulkItems) {
                        emitter.onComplete();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        );
    }

    @AllArgsConstructor
    private static class SubscriptionPage {

        private SubscriptionCursor cursor;
    }
}
