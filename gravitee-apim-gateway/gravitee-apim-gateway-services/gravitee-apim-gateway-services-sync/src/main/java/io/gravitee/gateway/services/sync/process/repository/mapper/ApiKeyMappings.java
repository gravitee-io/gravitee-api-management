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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApiKeyMappings {

    private ApiKeyMappings() {}

    /**
     * Drops the mappings of one key record that are inactive on a scope where the same record has an active mapping, such as
     * a shared key that is still linked to a closed subscription of a plan it has subscribed to again. The key cache evicts a
     * record through any of its mappings, so such a mapping registered after the active one would drop the key. Distributed
     * sync stores one event per scope and key record, so it would also replace the active one on the other gateways.
     */
    public static List<ApiKey> withoutInactiveMappingsWhereActive(final List<ApiKey> mappingsOfOneRecord) {
        Set<String> scopesWithActiveMapping = mappingsOfOneRecord
            .stream()
            .filter(ApiKey::isActive)
            .map(ApiKey::getApi)
            .collect(Collectors.toSet());
        return mappingsOfOneRecord
            .stream()
            .filter(apiKey -> apiKey.isActive() || !scopesWithActiveMapping.contains(apiKey.getApi()))
            .toList();
    }
}
