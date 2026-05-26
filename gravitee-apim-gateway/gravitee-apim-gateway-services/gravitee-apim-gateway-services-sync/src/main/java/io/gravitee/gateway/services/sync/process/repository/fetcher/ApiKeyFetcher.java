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

import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKey;
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
public class ApiKeyFetcher {

    private final ApiKeyRepository apiKeyRepository;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    public ApiKeyFetcher(ApiKeyRepository apiKeyRepository, int bulkItems) {
        if (bulkItems <= 0) {
            throw new IllegalArgumentException("bulkItems must be > 0 (got " + bulkItems + ")");
        }
        this.apiKeyRepository = apiKeyRepository;
        this.bulkItems = bulkItems;
    }

    public Flowable<List<ApiKey>> fetchLatest(final Long from, final Long to, final Set<String> environments) {
        ApiKeyCriteria criteria = ApiKeyCriteria.builder()
            .includeRevoked(true)
            .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
            .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
            .environments(environments)
            .build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        return Flowable.<List<ApiKey>, ApiKeyPage>generate(
            () -> new ApiKeyPage(null),
            (page, emitter) -> {
                try {
                    List<ApiKey> apiKeys = apiKeyRepository.searchAfter(criteria, sortable, page.cursor, bulkItems);
                    if (apiKeys != null && !apiKeys.isEmpty()) {
                        emitter.onNext(apiKeys);
                        ApiKey last = apiKeys.getLast();
                        if (last.getUpdatedAt() == null) {
                            // Criteria.from/to > 0 already filters rows lacking updatedAt at the
                            // repository layer. Guard the cursor advance so a malformed row never
                            // wedges the loop (NPE → onError → retry exhaustion → tick never
                            // completes → next tick refetches the same poison row).
                            log.warn("ApiKey {} has null updatedAt; terminating page loop early", last.getId());
                            emitter.onComplete();
                            return;
                        }
                        page.cursor = ApiKeyCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId());
                    }
                    if (apiKeys == null || apiKeys.size() < bulkItems) {
                        emitter.onComplete();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        );
    }

    @AllArgsConstructor
    private static class ApiKeyPage {

        private ApiKeyCursor cursor;
    }
}
