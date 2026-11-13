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
package io.gravitee.apim.infra.crud_service.secret;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.infra.adapter.SecretAdapter;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SecretCrudServiceImpl implements SecretCrudService {

    private final SecretRepository secretRepository;
    private final SecretAdapter secretAdapter;

    public SecretCrudServiceImpl(@Lazy SecretRepository secretRepository, SecretAdapter secretAdapter) {
        this.secretRepository = secretRepository;
        this.secretAdapter = secretAdapter;
    }

    @Override
    public Secret create(Secret secret, String encryptedValue) {
        try {
            return secretAdapter.fromRepository(secretRepository.create(secretAdapter.toRepository(secret, encryptedValue)));
        } catch (DuplicateKeyException e) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to create secret " + secret.id(), e);
        }
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) {
        try {
            return secretRepository.findById(organizationId, environmentId, id).map(secretAdapter::fromRepository);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to find secret " + id, e);
        }
    }

    @Override
    public String getEncryptedValue(Secret secret) {
        try {
            return secretRepository
                .findById(secret.organizationId(), secret.environmentId(), secret.id())
                .map(stored -> stored.getEncryptedValue())
                .orElseThrow(() -> new SecretNotFoundException(secret.id()));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to read secret " + secret.id(), e);
        }
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) {
        try {
            return secretRepository.findByKey(organizationId, environmentId, key).map(secretAdapter::fromRepository);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to find secret by key " + key, e);
        }
    }

    @Override
    public Secret update(Secret secret, String encryptedValue) {
        try {
            return secretAdapter.fromRepository(secretRepository.update(secretAdapter.toRepository(secret, encryptedValue)));
        } catch (TechnicalException e) {
            // The repository fails an update that matches no row, so a secret deleted since it was read is reported as missing.
            if (findById(secret.organizationId(), secret.environmentId(), secret.id()).isEmpty()) {
                throw new SecretNotFoundException(secret.id());
            }
            throw new TechnicalDomainException("Failed to update secret " + secret.id(), e);
        }
    }

    @Override
    public void delete(Secret secret) {
        try {
            secretRepository.delete(secret.organizationId(), secret.environmentId(), secret.id());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to delete secret " + secret.id(), e);
        }
    }
}
