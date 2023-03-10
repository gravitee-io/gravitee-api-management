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
package io.gravitee.gateway.services.sync.process.synchronizer.apikey;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.services.sync.process.model.ApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.model.SyncAction;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@Accessors(fluent = true)
public class SingleApikeyDeployable implements ApiKeyDeployable {

    @NonNull
    private ApiKey apiKey;

    private SyncAction syncAction;

    public String id() {
        return apiKey.getId();
    }

    public String apiId() {
        return apiKey.getApi();
    }

    public Set<String> apiKeyPlans() {
        return Set.of(apiKey.getPlan());
    }

    @Override
    public List<ApiKey> apiKeys() {
        return List.of(apiKey);
    }

    public SingleApikeyDeployable apiKeys(final List<ApiKey> apiKeys) {
        throw new UnsupportedOperationException(String.format("Unable to override apikey for %s", SingleApikeyDeployable.class.getName()));
    }
}
