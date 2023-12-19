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
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.LicenseCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.License;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LicenseFetcher {

    private final LicenseRepository licenseRepository;

    public Flowable<List<License>> fetchLatest(Long from, Long to) {
        return Flowable.generate(emitter -> {
            LicenseCriteria criteria = LicenseCriteria
                .builder()
                .referenceType(License.ReferenceType.ORGANIZATION)
                .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                .build();
            try {
                List<License> licenses = licenseRepository.findByCriteria(criteria);
                if (licenses != null && !licenses.isEmpty()) {
                    emitter.onNext(licenses);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
