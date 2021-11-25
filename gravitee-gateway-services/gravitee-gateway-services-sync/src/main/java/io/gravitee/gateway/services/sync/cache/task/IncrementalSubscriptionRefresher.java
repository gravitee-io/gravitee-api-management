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
package io.gravitee.gateway.services.sync.cache.task;

import static io.gravitee.repository.management.model.Subscription.Status.CLOSED;
import static io.gravitee.repository.management.model.Subscription.Status.PAUSED;

import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IncrementalSubscriptionRefresher extends SubscriptionRefresher {

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;

    private final Collection<String> plans;

    private final long lastRefreshAt, nextLastRefreshAt;

    private static final List<Subscription.Status> REFRESH_STATUS = Arrays.asList(Subscription.Status.ACCEPTED, CLOSED, PAUSED);

    public IncrementalSubscriptionRefresher(final long lastRefreshAt, final long nextLastRefreshAt, final List<String> plans) {
        this.lastRefreshAt = lastRefreshAt;
        this.nextLastRefreshAt = nextLastRefreshAt;
        this.plans = plans;
    }

    @Override
    public Result<Boolean> call() {
        return doRefresh(
            new SubscriptionCriteria.Builder()
                .plans(plans)
                .statuses(REFRESH_STATUS)
                .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
                .build()
        );
    }
}
