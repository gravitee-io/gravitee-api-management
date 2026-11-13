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
package inmemory;

import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SecretCrudServiceInMemory extends AbstractServiceInMemory<Secret> implements SecretCrudService {

    private final Map<String, String> encryptedValues = new HashMap<>();

    @Override
    public Secret create(Secret secret, String encryptedValue) {
        if (storage.stream().anyMatch(s -> s.environmentId().equals(secret.environmentId()) && s.key().equals(secret.key()))) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        }
        storage.add(secret);
        encryptedValues.put(secret.id(), encryptedValue);
        return secret;
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) {
        return storage
            .stream()
            .filter(s -> s.id().equals(id) && inScope(s, organizationId, environmentId))
            .findFirst();
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) {
        return storage
            .stream()
            .filter(s -> s.key().equals(key) && inScope(s, organizationId, environmentId))
            .findFirst();
    }

    @Override
    public Secret update(Secret secret, String encryptedValue) {
        var index = findIndex(
            storage,
            s -> s.id().equals(secret.id()) && inScope(s, secret.organizationId(), secret.environmentId())
        ).orElseThrow(() -> new SecretNotFoundException(secret.id()));
        var updated = storage.get(index).withUpdatedAt(secret.updatedAt());
        storage.set(index, updated);
        encryptedValues.put(secret.id(), encryptedValue);
        return updated;
    }

    @Override
    public String getEncryptedValue(Secret secret) {
        return findById(secret.organizationId(), secret.environmentId(), secret.id())
            .flatMap(found -> encryptedValueOf(found.id()))
            .orElseThrow(() -> new SecretNotFoundException(secret.id()));
    }

    @Override
    public void delete(Secret secret) {
        if (storage.removeIf(s -> s.id().equals(secret.id()) && inScope(s, secret.organizationId(), secret.environmentId()))) {
            encryptedValues.remove(secret.id());
        }
    }

    @Override
    public void reset() {
        super.reset();
        encryptedValues.clear();
    }

    public Optional<String> encryptedValueOf(String id) {
        return Optional.ofNullable(encryptedValues.get(id));
    }

    private static boolean inScope(Secret secret, String organizationId, String environmentId) {
        return secret.organizationId().equals(organizationId) && secret.environmentId().equals(environmentId);
    }
}
