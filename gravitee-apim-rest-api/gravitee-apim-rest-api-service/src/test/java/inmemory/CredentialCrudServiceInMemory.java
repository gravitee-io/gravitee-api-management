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

import io.gravitee.apim.core.credential.crud_service.CredentialCrudService;
import io.gravitee.apim.core.credential.model.Credential;
import java.util.Optional;

public class CredentialCrudServiceInMemory extends AbstractServiceInMemory<Credential> implements CredentialCrudService {

    private boolean failOnCreate;
    private boolean failOnUpdate;

    public void failOnCreate() {
        failOnCreate = true;
    }

    public void failOnUpdate() {
        failOnUpdate = true;
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
        if (failOnUpdate) {
            throw new IllegalStateException("credential store unavailable");
        }
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
}
