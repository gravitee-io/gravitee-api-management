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
package io.gravitee.gateway.services.sync.cache.task;

import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IncrementalApiKeyRefresher extends ApiKeyRefresher {

    private static final int TIMEFRAME_BEFORE_DELAY = 30_000;
    private static final int TIMEFRAME_AFTER_DELAY = 30_000;

    private final Collection<String> plans;

    private final long lastRefreshAt, nextLastRefreshAt;

    public IncrementalApiKeyRefresher(final long lastRefreshAt, final long nextLastRefreshAt, final List<String> plans) {
        this.lastRefreshAt = lastRefreshAt;
        this.nextLastRefreshAt = nextLastRefreshAt;
        this.plans = plans;
    }

    @Override
    public Result<Boolean> call() {
        return doRefresh(
            new ApiKeyCriteria.Builder()
                .plans(plans)
                .includeRevoked(true)
                .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
                .build()
        );
    }
}
