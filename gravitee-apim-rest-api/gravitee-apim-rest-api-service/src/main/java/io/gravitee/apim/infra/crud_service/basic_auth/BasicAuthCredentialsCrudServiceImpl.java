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
package io.gravitee.apim.infra.crud_service.basic_auth;

import io.gravitee.apim.core.basic_auth.crud_service.BasicAuthCredentialsCrudService;
import io.gravitee.apim.core.basic_auth.model.BasicAuthCredentialsEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.BasicAuthCredentialsRepository;
import io.gravitee.repository.management.model.BasicAuthCredentials;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class BasicAuthCredentialsCrudServiceImpl implements BasicAuthCredentialsCrudService {

    private final BasicAuthCredentialsRepository basicAuthCredentialsRepository;

    public BasicAuthCredentialsCrudServiceImpl(@Lazy BasicAuthCredentialsRepository basicAuthCredentialsRepository) {
        this.basicAuthCredentialsRepository = basicAuthCredentialsRepository;
    }

    @Override
    public BasicAuthCredentialsEntity create(BasicAuthCredentialsEntity credentials) {
        try {
            BasicAuthCredentials result = basicAuthCredentialsRepository.create(toRepository(credentials));
            return toEntity(result);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to create basic auth credentials [id={}]", credentials.getId(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to create basic auth credentials: " + credentials.getId(),
                ex
            );
        }
    }

    @Override
    public BasicAuthCredentialsEntity update(BasicAuthCredentialsEntity credentials) {
        try {
            BasicAuthCredentials result = basicAuthCredentialsRepository.update(toRepository(credentials));
            return toEntity(result);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to update basic auth credentials [id={}]", credentials.getId(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to update basic auth credentials: " + credentials.getId(),
                ex
            );
        }
    }

    @Override
    public Optional<BasicAuthCredentialsEntity> findBySubscriptionId(String subscriptionId) {
        try {
            return basicAuthCredentialsRepository.findBySubscription(subscriptionId).stream().findFirst().map(this::toEntity);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find basic auth credentials by subscription [id={}]", subscriptionId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to find basic auth credentials by subscription: " + subscriptionId,
                ex
            );
        }
    }

    private BasicAuthCredentials toRepository(BasicAuthCredentialsEntity entity) {
        if (entity == null) {
            return null;
        }
        return BasicAuthCredentials.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .password(entity.getPassword())
            .application(entity.getApplicationId())
            .subscriptions(entity.getSubscriptions() != null ? new ArrayList<>(entity.getSubscriptions()) : new ArrayList<>())
            .environmentId(entity.getEnvironmentId())
            .expireAt(toDate(entity.getExpireAt()))
            .createdAt(toDate(entity.getCreatedAt()))
            .updatedAt(toDate(entity.getUpdatedAt()))
            .revoked(entity.isRevoked())
            .revokedAt(toDate(entity.getRevokedAt()))
            .build();
    }

    private BasicAuthCredentialsEntity toEntity(BasicAuthCredentials repo) {
        if (repo == null) {
            return null;
        }
        return BasicAuthCredentialsEntity.builder()
            .id(repo.getId())
            .username(repo.getUsername())
            .password(repo.getPassword())
            .applicationId(repo.getApplication())
            .subscriptions(repo.getSubscriptions() != null ? new ArrayList<>(repo.getSubscriptions()) : new ArrayList<>())
            .environmentId(repo.getEnvironmentId())
            .expireAt(toZonedDateTime(repo.getExpireAt()))
            .createdAt(toZonedDateTime(repo.getCreatedAt()))
            .updatedAt(toZonedDateTime(repo.getUpdatedAt()))
            .revoked(repo.isRevoked())
            .revokedAt(toZonedDateTime(repo.getRevokedAt()))
            .build();
    }

    private static Date toDate(ZonedDateTime zonedDateTime) {
        return zonedDateTime != null ? Date.from(zonedDateTime.toInstant()) : null;
    }

    private static ZonedDateTime toZonedDateTime(Date date) {
        return date != null ? ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC) : null;
    }
}
