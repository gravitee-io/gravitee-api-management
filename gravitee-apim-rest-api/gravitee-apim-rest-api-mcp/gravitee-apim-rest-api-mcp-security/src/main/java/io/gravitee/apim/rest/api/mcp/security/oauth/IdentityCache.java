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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import io.gravitee.common.utils.TimeProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

/**
 * The identities behind the identity provider tokens already validated, so that an MCP session does not introspect on
 * every call. An entry lives until its token expires; a token revoked earlier stays accepted until then.
 *
 * <p>Keys are hashes: the cache never holds a token.
 */
public class IdentityCache<T> {

    private static final int MAX_IDENTITIES = 10_000;

    private final Cache<String, Entry<T>> entries = CacheBuilder.newBuilder().maximumSize(MAX_IDENTITIES).build();

    public Optional<T> get(String resource, String token) {
        var key = key(resource, token);
        var entry = entries.getIfPresent(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(TimeProvider.instantNow())) {
            entries.invalidate(key);
            return Optional.empty();
        }
        return Optional.of(entry.identity());
    }

    public void put(String resource, String token, T identity, Instant expiresAt) {
        entries.put(key(resource, token), new Entry<>(identity, expiresAt));
    }

    private static String key(String resource, String token) {
        return Hashing.sha256().hashString(resource + '\n' + token, StandardCharsets.UTF_8).toString();
    }

    private record Entry<T>(T identity, Instant expiresAt) {}
}
