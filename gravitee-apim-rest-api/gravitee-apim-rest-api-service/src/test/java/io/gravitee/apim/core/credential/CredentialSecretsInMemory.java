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
package io.gravitee.gamma.module.platform.inmemory;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialSecretStore;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fails a write when the secret key ends with a field given to {@link #failOn(String)}, and a delete when it ends with a
 * field given to {@link #failDeleteOf(String)}.
 */
public class InMemoryCredentialSecretStore implements CredentialSecretStore {

    private record Stored(String key, String value) {}

    private final Map<String, Stored> secrets = new LinkedHashMap<>();
    private final Set<String> failingFields = new HashSet<>();
    private final Set<String> failingDeletes = new HashSet<>();
    private int sequence;

    public void failOn(String field) {
        failingFields.add(field);
    }

    public void failDeleteOf(String field) {
        failingDeletes.add(field);
    }

    public Optional<String> valueOf(String key) {
        return secrets
            .values()
            .stream()
            .filter(s -> s.key().equals(key))
            .map(Stored::value)
            .findFirst();
    }

    public Set<String> keys() {
        var keys = new HashSet<String>();
        secrets.values().forEach(s -> keys.add(s.key()));
        return keys;
    }

    public void removeSecret(String id) {
        secrets.remove(id);
    }

    @Override
    public StoredSecret create(String key, String value, AuditInfo auditInfo) {
        failIfAsked(key);
        var id = "secret-" + (++sequence);
        secrets.put(id, new Stored(key, value));
        return new StoredSecret(id, "/gravitee/" + key + ":value");
    }

    @Override
    public void update(String secretId, String value, AuditInfo auditInfo) {
        var existing = secrets.get(secretId);
        if (existing == null) {
            throw new IllegalStateException("no secret " + secretId);
        }
        failIfAsked(existing.key());
        secrets.put(secretId, new Stored(existing.key(), value));
    }

    @Override
    public void delete(String secretId, AuditInfo auditInfo) {
        var existing = secrets.get(secretId);
        if (existing != null && failingDeletes.stream().anyMatch(field -> existing.key().endsWith("." + field))) {
            throw new IllegalStateException("secret store cannot delete " + existing.key());
        }
        secrets.remove(secretId);
    }

    private void failIfAsked(String key) {
        if (failingFields.stream().anyMatch(field -> key.endsWith("." + field))) {
            throw new IllegalStateException("secret store unavailable for " + key);
        }
    }
}
