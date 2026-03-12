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
package io.gravitee.gateway.handlers.api.services.basicauth;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

@CustomLog
public class BasicAuthCacheService {

    private final Map<String, BasicAuthCredential> cacheByApiAndUsername = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> cacheKeysByApi = new ConcurrentHashMap<>();

    public void register(final BasicAuthCredential credential) {
        if (credential.isActive()) {
            String cacheKey = buildCacheKey(credential.getApi(), credential.getUsername());
            log.debug(
                "Load active basic-auth credential [id: {}] [api: {}] [plan: {}] [app: {}]",
                credential.getId(),
                credential.getApi(),
                credential.getPlan(),
                credential.getApplication()
            );
            cacheByApiAndUsername.put(cacheKey, credential);
            cacheKeysByApi.computeIfAbsent(credential.getApi(), k -> new HashSet<>()).add(cacheKey);
        } else {
            unregister(credential);
        }
    }

    public void unregister(final BasicAuthCredential credential) {
        String cacheKey = buildCacheKey(credential.getApi(), credential.getUsername());
        log.debug("Unload basic-auth credential [id: {}] [api: {}]", credential.getId(), credential.getApi());
        if (cacheByApiAndUsername.remove(cacheKey) != null) {
            Set<String> keysByApi = cacheKeysByApi.get(credential.getApi());
            if (keysByApi != null && keysByApi.remove(cacheKey)) {
                if (keysByApi.isEmpty()) {
                    cacheKeysByApi.remove(credential.getApi());
                }
            }
        }
    }

    public void unregisterByApiId(final String apiId) {
        log.debug("Unload all basic-auth credentials by api [api_id: {}]", apiId);
        Set<String> keysByApi = cacheKeysByApi.remove(apiId);
        if (keysByApi != null) {
            keysByApi.forEach(cacheByApiAndUsername::remove);
        }
    }

    public Optional<BasicAuthCredential> getByApiAndUsername(String api, String username) {
        return Optional.ofNullable(cacheByApiAndUsername.get(buildCacheKey(api, username)));
    }

    String buildCacheKey(String api, String username) {
        return String.format("%s.%s", api, username);
    }
}
