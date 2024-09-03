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

import io.gravitee.common.data.domain.Page;
import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.search.LicenseCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.License;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
public class LicenseFetcher {

    private final LicenseRepository licenseRepository;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    public Flowable<List<License>> fetchLatest(final Long from, final Long to, final Set<String> organizations) {
        return Flowable.generate(
            () ->
                LicensePageable
                    .builder()
                    .index(0)
                    .size(bulkItems)
                    .criteria(
                        LicenseCriteria
                            .builder()
                            .referenceType(License.ReferenceType.ORGANIZATION)
                            .referenceIds(organizations)
                            .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                            .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                            .build()
                    )
                    .build(),
            (page, emitter) -> {
                try {
                    Page<License> licenses = licenseRepository.findByCriteria(
                        page.criteria,
                        new PageableBuilder().pageNumber(page.index).pageSize(page.size).build()
                    );
                    if (licenses != null && !licenses.getContent().isEmpty()) {
                        emitter.onNext(licenses.getContent());
                        page.index++;
                    }
                    if (licenses == null || licenses.getContent().size() < page.size) {
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
    private static class LicensePageable {

        private int index;
        private int size;
        private LicenseCriteria criteria;
    }
}
