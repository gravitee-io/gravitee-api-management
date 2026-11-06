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
package io.gravitee.gamma.module.platform.infra.repository.secret;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.port.repository.SecretRepository;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApimSecretRepository implements SecretRepository {

    private final io.gravitee.repository.management.api.SecretRepository secretRepository;

    @Override
    public Secret create(Secret secret, String encryptedValue) {
        try {
            return toCore(secretRepository.create(toStored(secret, encryptedValue)));
        } catch (DuplicateKeyException e) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to create secret " + secret.id(), e);
        }
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) {
        try {
            return secretRepository.findById(organizationId, environmentId, id).map(ApimSecretRepository::toCore);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to find secret " + id, e);
        }
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) {
        try {
            return secretRepository.findByKey(organizationId, environmentId, key).map(ApimSecretRepository::toCore);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to find secret by key " + key, e);
        }
    }

    @Override
    public Secret rotate(Secret secret, String encryptedValue) {
        try {
            return toCore(secretRepository.update(toStored(secret, encryptedValue)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to rotate secret " + secret.id(), e);
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

    @Override
    public Page<Secret> search(String organizationId, String environmentId, int pageNumber, int pageSize) {
        try {
            var page = secretRepository.search(
                organizationId,
                environmentId,
                new PageableBuilder().pageNumber(pageNumber - 1).pageSize(pageSize).build()
            );
            var content = page.getContent().stream().map(ApimSecretRepository::toCore).toList();
            return new Page<>(content, pageNumber, content.size(), page.getTotalElements());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to search the secrets of environment " + environmentId, e);
        }
    }

    private static io.gravitee.repository.management.model.Secret toStored(Secret secret, String encryptedValue) {
        return io.gravitee.repository.management.model.Secret.builder()
            .id(secret.id())
            .organizationId(secret.organizationId())
            .environmentId(secret.environmentId())
            .key(secret.key())
            .encryptedValue(encryptedValue)
            .createdAt(Date.from(secret.createdAt()))
            .updatedAt(Date.from(secret.updatedAt()))
            .build();
    }

    private static Secret toCore(io.gravitee.repository.management.model.Secret stored) {
        return new Secret(
            stored.getId(),
            stored.getOrganizationId(),
            stored.getEnvironmentId(),
            stored.getKey(),
            toInstant(stored.getCreatedAt()),
            toInstant(stored.getUpdatedAt())
        );
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
