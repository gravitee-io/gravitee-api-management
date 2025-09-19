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
package io.gravitee.gateway.services.sync.process.local.mapper;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import java.util.Set;

public class ApiKeyMapper {

    public ApiKey to(io.gravitee.repository.management.model.ApiKey apiKeyModel, Set<Subscription> subscriptions) {
        ApiKey.ApiKeyBuilder apiKeyBuilder = ApiKey.builder()
            .id(apiKeyModel.getId())
            .key(apiKeyModel.getKey())
            .application(apiKeyModel.getApplication())
            .expireAt(apiKeyModel.getExpireAt())
            .revoked(apiKeyModel.isRevoked())
            .paused(apiKeyModel.isPaused())
            .active(false);

        if (subscriptions != null) {
            subscriptions
                .stream()
                .filter(subscription -> apiKeyModel.getSubscriptions().contains(subscription.getId()))
                .findFirst()
                .ifPresent(apiKeySubscription ->
                    apiKeyBuilder
                        .api(apiKeySubscription.getApi())
                        .plan(apiKeySubscription.getPlan())
                        .subscription(apiKeySubscription.getId())
                        .active(
                            !apiKeyModel.isPaused() &&
                                !apiKeyModel.isRevoked() &&
                                io.gravitee.repository.management.model.Subscription.Status.ACCEPTED.name().equals(
                                    apiKeySubscription.getStatus()
                                )
                        )
                );
        }
        return apiKeyBuilder.build();
    }
}
