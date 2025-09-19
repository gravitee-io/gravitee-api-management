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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiKeyMapper {

    public ApiKey to(io.gravitee.repository.management.model.ApiKey apiKeyModel, Subscription subscription) {
        ApiKey.ApiKeyBuilder apiKeyBuilder = ApiKey.builder()
            .id(apiKeyModel.getId())
            .key(apiKeyModel.getKey())
            .application(apiKeyModel.getApplication())
            .expireAt(apiKeyModel.getExpireAt())
            .revoked(apiKeyModel.isRevoked())
            .paused(apiKeyModel.isPaused());
        if (subscription != null) {
            apiKeyBuilder
                .api(subscription.getApi())
                .plan(subscription.getPlan())
                .subscription(subscription.getId())
                .active(
                    !apiKeyModel.isPaused() &&
                        !apiKeyModel.isRevoked() &&
                        io.gravitee.repository.management.model.Subscription.Status.ACCEPTED.name().equals(subscription.getStatus())
                );
        } else {
            apiKeyBuilder.active(false);
        }
        return apiKeyBuilder.build();
    }

    public ApiKey to(io.gravitee.repository.management.model.ApiKey apiKeyModel, Optional<Subscription> optionalSubscription) {
        return to(apiKeyModel, optionalSubscription.orElse(null));
    }
}
