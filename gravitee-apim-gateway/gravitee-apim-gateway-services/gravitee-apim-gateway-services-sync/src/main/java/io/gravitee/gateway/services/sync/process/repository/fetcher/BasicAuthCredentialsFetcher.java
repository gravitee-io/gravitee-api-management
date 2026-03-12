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
import io.gravitee.repository.management.api.BasicAuthCredentialsRepository;
import io.gravitee.repository.management.api.search.BasicAuthCredentialsCriteria;
import io.gravitee.repository.management.model.BasicAuthCredentials;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BasicAuthCredentialsFetcher {

    private final BasicAuthCredentialsRepository basicAuthCredentialsRepository;

    public Flowable<List<BasicAuthCredentials>> fetchLatest(final Long from, final Long to, final Set<String> environments) {
        return Flowable.generate(emitter -> {
            BasicAuthCredentialsCriteria criteria = BasicAuthCredentialsCriteria.builder()
                .includeRevoked(true)
                .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                .environments(environments)
                .build();
            try {
                List<BasicAuthCredentials> credentials = basicAuthCredentialsRepository.findByCriteria(criteria);
                if (credentials != null && !credentials.isEmpty()) {
                    emitter.onNext(credentials);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
