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

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.port.repository.CredentialRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class InMemoryCredentialRepository implements CredentialRepository {

    private final List<Credential> storage = new ArrayList<>();
    private boolean failOnCreate;

    public void failOnCreate() {
        failOnCreate = true;
    }

    public List<Credential> storage() {
        return List.copyOf(storage);
    }

    @Override
    public Credential create(Credential credential) {
        if (failOnCreate) {
            throw new IllegalStateException("credential store unavailable");
        }
        storage.add(credential);
        return credential;
    }

    @Override
    public Optional<Credential> findById(String organizationId, String environmentId, String id) {
        return storage
            .stream()
            .filter(c -> c.id().equals(id) && c.organizationId().equals(organizationId) && c.environmentId().equals(environmentId))
            .findFirst();
    }

    @Override
    public Credential update(Credential credential) {
        var existing = findById(credential.organizationId(), credential.environmentId(), credential.id()).orElseThrow();
        storage.set(storage.indexOf(existing), credential);
        return credential;
    }

    @Override
    public void delete(Credential credential) {
        storage.removeIf(
            c ->
                c.id().equals(credential.id()) &&
                c.organizationId().equals(credential.organizationId()) &&
                c.environmentId().equals(credential.environmentId())
        );
    }

    @Override
    public Page<Credential> search(String organizationId, String environmentId, String query, int pageNumber, int pageSize) {
        var matching = storage
            .stream()
            .filter(c -> c.organizationId().equals(organizationId) && c.environmentId().equals(environmentId))
            .filter(c -> query == null || c.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(Credential::name).thenComparing(Credential::id))
            .toList();
        var content = matching.stream().skip((long) pageNumber * pageSize).limit(pageSize).toList();
        return new Page<>(content, pageNumber, content.size(), matching.size());
    }
}
