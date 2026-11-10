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
package io.gravitee.apim.infra.crud_service.credential;

import io.gravitee.apim.core.credential.crud_service.CredentialCrudService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.CredentialAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CredentialRepository;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CredentialCrudServiceImpl implements CredentialCrudService {

    private final CredentialRepository credentialRepository;

    public CredentialCrudServiceImpl(@Lazy CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    public Credential create(Credential credential) {
        try {
            return CredentialAdapter.INSTANCE.toCoreModel(credentialRepository.create(CredentialAdapter.INSTANCE.toRepository(credential)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to create credential " + credential.id(), e);
        }
    }

    @Override
    public Optional<Credential> findById(String organizationId, String environmentId, String id) {
        try {
            return credentialRepository.findById(organizationId, environmentId, id).map(CredentialAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to read credential " + id, e);
        }
    }

    @Override
    public Credential update(Credential credential) {
        try {
            return CredentialAdapter.INSTANCE.toCoreModel(credentialRepository.update(CredentialAdapter.INSTANCE.toRepository(credential)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to update credential " + credential.id(), e);
        }
    }

    @Override
    public void delete(Credential credential) {
        try {
            credentialRepository.delete(credential.organizationId(), credential.environmentId(), credential.id());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to delete credential " + credential.id(), e);
        }
    }
}
