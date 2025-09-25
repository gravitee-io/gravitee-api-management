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
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
public class AccessPointFetcher {

    private final AccessPointRepository accessPointRepository;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    public Flowable<List<AccessPoint>> fetchLatest(Long from, Long to, Set<String> environments, AccessPointStatus status) {
        return Flowable.generate(
            () ->
                AccessPointPageable.builder()
                    .index(0)
                    .size(bulkItems)
                    .criteria(
                        AccessPointCriteria.builder()
                            .referenceType(AccessPointReferenceType.ENVIRONMENT)
                            .targets(List.of(AccessPointTarget.GATEWAY, AccessPointTarget.TCP_GATEWAY, AccessPointTarget.KAFKA_GATEWAY))
                            .referenceIds(environments)
                            .status(status)
                            .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                            .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                            .build()
                    )
                    .build(),
            (page, emitter) -> {
                try {
                    List<AccessPoint> accessPoints = accessPointRepository.findByCriteria(page.criteria, page.index, page.size);
                    if (accessPoints != null && !accessPoints.isEmpty()) {
                        emitter.onNext(accessPoints);
                        page.index++;
                    }
                    if (accessPoints == null || accessPoints.size() < page.size) {
                        emitter.onComplete();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        );
    }

    @Builder
    @AllArgsConstructor
    @Getter
    private static class AccessPointPageable {

        private long index;
        private long size;
        private AccessPointCriteria criteria;
    }
}
