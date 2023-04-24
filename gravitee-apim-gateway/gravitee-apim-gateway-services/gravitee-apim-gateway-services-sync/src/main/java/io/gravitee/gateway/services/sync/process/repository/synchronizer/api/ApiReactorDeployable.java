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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.ApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SubscriptionDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public class ApiReactorDeployable implements ApiKeyDeployable, SubscriptionDeployable {

    private String apiId;

    private ReactableApi<?> reactableApi;

    private SyncAction syncAction;

    @Builder.Default
    private List<Subscription> subscriptions = List.of();

    @Builder.Default
    private List<ApiKey> apiKeys = List.of();

    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private Set<String> subscribablePlans;

    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private Set<String> apiKeyPlans;

    @Override
    public String id() {
        return apiId;
    }

    public String apiId() {
        if (apiId == null) {
            return reactableApi.getId();
        }
        return apiId;
    }

    public Set<String> subscribablePlans() {
        if (subscribablePlans == null) {
            if (reactableApi != null) {
                Set<String> reactableApiSubscribablePlans = reactableApi.getSubscribablePlans();
                if (reactableApiSubscribablePlans != null) {
                    subscribablePlans = new HashSet<>();
                    subscribablePlans.addAll(reactableApiSubscribablePlans);
                    return subscribablePlans;
                }
            }
            return Set.of();
        }
        return subscribablePlans;
    }

    public Set<String> apiKeyPlans() {
        if (apiKeyPlans == null) {
            if (reactableApi != null) {
                Set<String> reactableApiApiKeyPlans = reactableApi.getApiKeyPlans();
                if (reactableApiApiKeyPlans != null) {
                    apiKeyPlans = new HashSet<>();
                    apiKeyPlans.addAll(reactableApiApiKeyPlans);
                    return apiKeyPlans;
                }
            }
            return Set.of();
        }
        return apiKeyPlans;
    }
}
