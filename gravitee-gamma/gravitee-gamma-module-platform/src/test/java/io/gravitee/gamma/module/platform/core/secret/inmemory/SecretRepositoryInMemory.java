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
package io.gravitee.gamma.module.platform.core.secret.inmemory;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.gamma.module.platform.core.secret.exception.SecretNotFoundException;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SecretRepositoryInMemory implements SecretRepository {

    private record Stored(Secret secret, String encryptedValue) {}

    private final Map<String, Stored> storage = new LinkedHashMap<>();

    @Override
    public Secret create(Secret secret, String encryptedValue) {
        boolean keyTaken = storage
            .values()
            .stream()
            .anyMatch(s -> s.secret().environmentId().equals(secret.environmentId()) && s.secret().key().equals(secret.key()));
        if (keyTaken) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        }
        storage.put(secret.id(), new Stored(secret, encryptedValue));
        return secret;
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) {
        return Optional.ofNullable(storage.get(id))
            .map(Stored::secret)
            .filter(s -> inScope(s, organizationId, environmentId));
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) {
        return storage
            .values()
            .stream()
            .map(Stored::secret)
            .filter(s -> inScope(s, organizationId, environmentId) && s.key().equals(key))
            .findFirst();
    }

    @Override
    public Secret rotate(Secret secret, String encryptedValue) {
        var current = findById(secret.organizationId(), secret.environmentId(), secret.id()).orElseThrow(() ->
            new SecretNotFoundException(secret.id())
        );
        var rotated = current.toBuilder().updatedAt(secret.updatedAt()).build();
        storage.put(secret.id(), new Stored(rotated, encryptedValue));
        return rotated;
    }

    @Override
    public void delete(Secret secret) {
        findById(secret.organizationId(), secret.environmentId(), secret.id()).ifPresent(s -> storage.remove(s.id()));
    }

    @Override
    public Page<Secret> search(String organizationId, String environmentId, int pageNumber, int pageSize) {
        var all = storage
            .values()
            .stream()
            .map(Stored::secret)
            .filter(s -> inScope(s, organizationId, environmentId))
            .sorted(Comparator.comparing(Secret::key))
            .toList();
        var content = all.stream().skip((long) (pageNumber - 1) * pageSize).limit(pageSize).toList();
        return new Page<>(content, pageNumber, content.size(), all.size());
    }

    public Optional<String> encryptedValueOf(String id) {
        return Optional.ofNullable(storage.get(id)).map(Stored::encryptedValue);
    }

    private static boolean inScope(Secret secret, String organizationId, String environmentId) {
        return secret.organizationId().equals(organizationId) && secret.environmentId().equals(environmentId);
    }
}
