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
package io.gravitee.gamma.module.platform.infra.repository.credential;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.port.repository.CredentialRepository;
import io.gravitee.gamma.module.platform.infra.adapter.CredentialAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/** Keeps the credentials in the APIM management repository, so they live on Mongo or JDBC like every other entity. */
@RequiredArgsConstructor
public class ApimCredentialRepository implements CredentialRepository {

    private final io.gravitee.repository.management.api.CredentialRepository repository;

    @Override
    public Credential create(Credential credential) {
        try {
            return CredentialAdapter.INSTANCE.toCoreModel(repository.create(CredentialAdapter.INSTANCE.toRepository(credential)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Could not create credential " + credential.id(), e);
        }
    }

    @Override
    public Optional<Credential> findById(String organizationId, String environmentId, String id) {
        try {
            return repository.findById(organizationId, environmentId, id).map(CredentialAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Could not read credential " + id, e);
        }
    }

    @Override
    public Credential update(Credential credential) {
        try {
            return CredentialAdapter.INSTANCE.toCoreModel(repository.update(CredentialAdapter.INSTANCE.toRepository(credential)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Could not update credential " + credential.id(), e);
        }
    }

    @Override
    public void delete(Credential credential) {
        try {
            repository.delete(credential.organizationId(), credential.environmentId(), credential.id());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Could not delete credential " + credential.id(), e);
        }
    }

    @Override
    public Page<Credential> search(String organizationId, String environmentId, String query, int pageNumber, int pageSize) {
        try {
            var page = repository.search(
                organizationId,
                environmentId,
                query,
                new PageableBuilder().pageNumber(pageNumber).pageSize(pageSize).build()
            );
            return page.map(CredentialAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Could not search the credentials of environment " + environmentId, e);
        }
    }
}
