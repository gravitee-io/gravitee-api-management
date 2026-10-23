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
package io.gravitee.apim.core.api_key.query_service;

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.model.ExpiringApiKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface ApiKeyQueryService {
    Optional<ApiKeyEntity> findById(String apiKeyId);
    Stream<ApiKeyEntity> findByApplication(String applicationId);
    Optional<ApiKeyEntity> findByKeyAndApiId(String key, String apiId);
    Stream<ApiKeyEntity> findBySubscription(String subscriptionId);
    Optional<ApiKeyEntity> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType);

    /**
     * Returns API keys whose {@code expireAt} falls inside any of the per-bucket windows
     * {@code [now + d*24h, now + d*24h + windowMs)} for each {@code d} in {@code daysBuckets}.
     * Runs as a single repository query over the outer-union range; callers perform bucket-inference
     * in memory. Non-revoked keys only. Federated keys included.
     */
    List<ExpiringApiKey> findExpiringApiKeys(Instant now, List<Integer> daysBuckets, long windowMs);
}
