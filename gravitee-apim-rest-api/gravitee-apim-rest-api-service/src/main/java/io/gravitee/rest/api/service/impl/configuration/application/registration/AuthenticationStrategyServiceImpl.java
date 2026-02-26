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
package io.gravitee.rest.api.service.impl.configuration.application.registration;

import static io.gravitee.repository.management.model.Audit.AuditProperties.AUTHENTICATION_STRATEGY;
import static io.gravitee.repository.management.model.AuthenticationStrategy.AuditEvent.*;
import static java.util.Collections.singletonMap;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuthenticationStrategyRepository;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.AuthenticationStrategy;
import io.gravitee.rest.api.model.configuration.application.registration.AuthenticationStrategyEntity;
import io.gravitee.rest.api.model.configuration.application.registration.AuthenticationStrategyType;
import io.gravitee.rest.api.model.configuration.application.registration.NewAuthenticationStrategyEntity;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateAuthenticationStrategyEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.application.AuthenticationStrategyService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class AuthenticationStrategyServiceImpl extends AbstractService implements AuthenticationStrategyService {

    @Lazy
    @Autowired
    private AuthenticationStrategyRepository authenticationStrategyRepository;

    @Lazy
    @Autowired
    private ClientRegistrationProviderRepository clientRegistrationProviderRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public Set<AuthenticationStrategyEntity> findAll(ExecutionContext executionContext) {
        try {
            return authenticationStrategyRepository
                .findAllByEnvironment(executionContext.getEnvironmentId())
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find all authentication strategies", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all authentication strategies", ex);
        }
    }

    @Override
    public AuthenticationStrategyEntity findById(String environmentId, String id) {
        try {
            Optional<AuthenticationStrategy> opt = authenticationStrategyRepository.findById(id);
            if (opt.isEmpty()) {
                throw new IllegalStateException("No authentication strategy found with id: " + id);
            }
            return convert(opt.get());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find authentication strategy {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find authentication strategy " + id, ex);
        }
    }

    @Override
    public AuthenticationStrategyEntity create(
        ExecutionContext executionContext,
        NewAuthenticationStrategyEntity newStrategy
    ) {
        try {
            log.debug("Create authentication strategy {}", newStrategy);

            validateProviderReference(newStrategy.getType(), newStrategy.getClientRegistrationProviderId());

            AuthenticationStrategy strategy = convert(newStrategy);
            strategy.setId(UuidString.generateRandom());
            strategy.setEnvironmentId(executionContext.getEnvironmentId());
            strategy.setCreatedAt(new Date());
            strategy.setUpdatedAt(strategy.getCreatedAt());

            AuthenticationStrategy created = authenticationStrategyRepository.create(strategy);

            auditService.createAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(singletonMap(AUTHENTICATION_STRATEGY, created.getId()))
                    .event(AUTHENTICATION_STRATEGY_CREATED)
                    .createdAt(created.getUpdatedAt())
                    .oldValue(null)
                    .newValue(created)
                    .build()
            );

            return convert(created);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to create authentication strategy {}", newStrategy, ex);
            throw new TechnicalManagementException("An error occurs while trying to create authentication strategy", ex);
        }
    }

    @Override
    public AuthenticationStrategyEntity update(
        ExecutionContext executionContext,
        String id,
        UpdateAuthenticationStrategyEntity updateStrategy
    ) {
        try {
            log.debug("Update authentication strategy {}", updateStrategy);

            Optional<AuthenticationStrategy> opt = authenticationStrategyRepository.findById(id);
            if (opt.isEmpty()) {
                throw new IllegalStateException("No authentication strategy found with id: " + id);
            }

            AuthenticationStrategy existing = opt.get();
            validateProviderReference(updateStrategy.getType(), updateStrategy.getClientRegistrationProviderId());

            AuthenticationStrategy toUpdate = convert(updateStrategy);
            toUpdate.setId(id);
            toUpdate.setEnvironmentId(existing.getEnvironmentId());
            toUpdate.setCreatedAt(existing.getCreatedAt());
            toUpdate.setUpdatedAt(new Date());

            AuthenticationStrategy updated = authenticationStrategyRepository.update(toUpdate);

            auditService.createAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(singletonMap(AUTHENTICATION_STRATEGY, updated.getId()))
                    .event(AUTHENTICATION_STRATEGY_UPDATED)
                    .createdAt(updated.getUpdatedAt())
                    .oldValue(existing)
                    .newValue(updated)
                    .build()
            );

            return convert(updated);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to update authentication strategy {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to update authentication strategy", ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String id) {
        try {
            log.debug("Delete authentication strategy {}", id);

            Optional<AuthenticationStrategy> opt = authenticationStrategyRepository.findById(id);
            if (opt.isEmpty()) {
                throw new IllegalStateException("No authentication strategy found with id: " + id);
            }

            authenticationStrategyRepository.delete(id);

            auditService.createAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(singletonMap(AUTHENTICATION_STRATEGY, id))
                    .event(AUTHENTICATION_STRATEGY_DELETED)
                    .createdAt(new Date())
                    .oldValue(opt.get())
                    .newValue(null)
                    .build()
            );
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to delete authentication strategy {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete authentication strategy", ex);
        }
    }

    @Override
    public Set<AuthenticationStrategyEntity> findByClientRegistrationProviderId(
        ExecutionContext executionContext,
        String clientRegistrationProviderId
    ) {
        try {
            return authenticationStrategyRepository
                .findByClientRegistrationProviderId(clientRegistrationProviderId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find strategies by provider {}", clientRegistrationProviderId, ex);
            throw new TechnicalManagementException("An error occurs while finding strategies by provider", ex);
        }
    }

    private void validateProviderReference(AuthenticationStrategyType type, String providerId) {
        if (type == AuthenticationStrategyType.DCR || type == AuthenticationStrategyType.SELF_MANAGED_OIDC) {
            if (providerId == null || providerId.isEmpty()) {
                throw new IllegalArgumentException(
                    "A client registration provider ID is required for " + type + " authentication strategies"
                );
            }
            try {
                if (clientRegistrationProviderRepository.findById(providerId).isEmpty()) {
                    throw new IllegalStateException("No client registration provider found with id: " + providerId);
                }
            } catch (TechnicalException ex) {
                throw new TechnicalManagementException("Failed to validate provider reference", ex);
            }
        }
    }

    private AuthenticationStrategyEntity convert(AuthenticationStrategy strategy) {
        AuthenticationStrategyEntity entity = new AuthenticationStrategyEntity();
        entity.setId(strategy.getId());
        entity.setName(strategy.getName());
        entity.setDisplayName(strategy.getDisplayName());
        entity.setDescription(strategy.getDescription());
        entity.setType(strategy.getType() != null ? AuthenticationStrategyType.valueOf(strategy.getType().name()) : null);
        entity.setClientRegistrationProviderId(strategy.getClientRegistrationProviderId());
        entity.setScopes(strategy.getScopes());
        entity.setAuthMethods(strategy.getAuthMethods());
        entity.setCredentialClaims(strategy.getCredentialClaims());
        entity.setAutoApprove(strategy.isAutoApprove());
        entity.setHideCredentials(strategy.isHideCredentials());
        entity.setCreatedAt(strategy.getCreatedAt());
        entity.setUpdatedAt(strategy.getUpdatedAt());
        return entity;
    }

    private AuthenticationStrategy convert(NewAuthenticationStrategyEntity entity) {
        AuthenticationStrategy strategy = new AuthenticationStrategy();
        strategy.setName(entity.getName());
        strategy.setDisplayName(entity.getDisplayName());
        strategy.setDescription(entity.getDescription());
        strategy.setType(entity.getType() != null ? AuthenticationStrategy.Type.valueOf(entity.getType().name()) : null);
        strategy.setClientRegistrationProviderId(entity.getClientRegistrationProviderId());
        strategy.setScopes(entity.getScopes());
        strategy.setAuthMethods(entity.getAuthMethods());
        strategy.setCredentialClaims(entity.getCredentialClaims());
        strategy.setAutoApprove(entity.isAutoApprove());
        strategy.setHideCredentials(entity.isHideCredentials());
        return strategy;
    }

    private AuthenticationStrategy convert(UpdateAuthenticationStrategyEntity entity) {
        AuthenticationStrategy strategy = new AuthenticationStrategy();
        strategy.setName(entity.getName());
        strategy.setDisplayName(entity.getDisplayName());
        strategy.setDescription(entity.getDescription());
        strategy.setType(entity.getType() != null ? AuthenticationStrategy.Type.valueOf(entity.getType().name()) : null);
        strategy.setClientRegistrationProviderId(entity.getClientRegistrationProviderId());
        strategy.setScopes(entity.getScopes());
        strategy.setAuthMethods(entity.getAuthMethods());
        strategy.setCredentialClaims(entity.getCredentialClaims());
        strategy.setAutoApprove(entity.isAutoApprove());
        strategy.setHideCredentials(entity.isHideCredentials());
        return strategy;
    }
}
