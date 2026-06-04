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
package io.gravitee.gamma.authorization.service;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzAuditEntry;
import io.gravitee.gamma.authorization.api.AuthzAuditPort;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.AuthzPolicyAdminApi;
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditEvent;
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditSnapshot;
import io.gravitee.gamma.authorization.api.AuthzPolicyRepository;
import io.gravitee.gamma.authorization.api.AuthzValidators;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyStatus;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidStatusTransitionException;
import io.gravitee.gamma.authorization.service.exception.AuthzPolicyNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AuthzPolicyServiceImpl implements AuthzPolicyAdminApi {

    private final AuthzPolicyRepository repository;
    private final AuthzEntityIdValidator entityIdValidator;
    private final AuthzEventPublisher eventPublisher;
    private final AuthzAuditPort auditPort;

    public AuthzPolicyServiceImpl(
        AuthzPolicyRepository repository,
        AuthzEntityIdValidator entityIdValidator,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        this.repository = repository;
        this.entityIdValidator = entityIdValidator;
        this.eventPublisher = eventPublisher;
        this.auditPort = auditPort;
    }

    @Override
    public AuthzPolicy create(AuthzCallerContext caller, CreateAuthzPolicyCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        AuthzValidators.requireMatchingEnv(caller, command.environmentId());
        entityIdValidator.validate(command.kind(), command.entityId());

        Instant now = TimeProvider.instantNow();

        AuthzPolicy policy = new AuthzPolicy(
            UUID.randomUUID().toString(),
            command.name(),
            command.kind(),
            command.entityId(),
            command.policyText() == null ? "" : command.policyText(),
            AuthzPolicyStatus.DRAFT,
            command.environmentId(),
            now,
            now
        );

        AuthzPolicy saved = repository.save(policy);
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_CREATED, saved.id(), null, AuthzPolicyAuditSnapshot.of(saved))
            );
        }
        return saved;
    }

    @Override
    public AuthzPolicy update(AuthzCallerContext caller, String id, UpdateAuthzPolicyCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        AuthzValidators.requireNonBlank(id, "id");
        Objects.requireNonNull(command, "command must not be null");

        AuthzPolicy existing = repository
            .findById(caller.environmentId(), id)
            .orElseThrow(() -> new AuthzPolicyNotFoundException(caller.environmentId(), id));

        AuthzPolicy updated = new AuthzPolicy(
            existing.id(),
            command.name() == null ? existing.name() : command.name(),
            existing.kind(),
            existing.entityId(),
            command.policyText() == null ? existing.policyText() : command.policyText(),
            existing.status(),
            existing.environmentId(),
            existing.createdAt(),
            TimeProvider.instantNow()
        );
        AuthzPolicy saved = repository.save(updated);
        if (saved.status() == AuthzPolicyStatus.DEPLOYED) {
            eventPublisher.publishPolicyDeployed(saved);
        }
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(
                    caller,
                    AuthzPolicyAuditEvent.POLICY_UPDATED,
                    saved.id(),
                    AuthzPolicyAuditSnapshot.of(existing),
                    AuthzPolicyAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    public AuthzPolicy deploy(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        AuthzValidators.requireNonBlank(id, "id");

        AuthzPolicy existing = repository
            .findById(caller.environmentId(), id)
            .orElseThrow(() -> new AuthzPolicyNotFoundException(caller.environmentId(), id));

        if (existing.status() == AuthzPolicyStatus.DEPLOYED) {
            return existing;
        }
        AuthzPolicy deployed = transitionTo(existing, AuthzPolicyStatus.DEPLOYED);
        AuthzPolicy saved = repository.save(deployed);
        eventPublisher.publishPolicyDeployed(saved);
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(
                    caller,
                    AuthzPolicyAuditEvent.POLICY_DEPLOYED,
                    saved.id(),
                    AuthzPolicyAuditSnapshot.of(existing),
                    AuthzPolicyAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    public AuthzPolicy disable(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        AuthzValidators.requireNonBlank(id, "id");

        AuthzPolicy existing = repository
            .findById(caller.environmentId(), id)
            .orElseThrow(() -> new AuthzPolicyNotFoundException(caller.environmentId(), id));

        if (existing.status() == AuthzPolicyStatus.DISABLED) {
            return existing;
        }
        if (existing.status() != AuthzPolicyStatus.DEPLOYED) {
            throw new AuthzInvalidStatusTransitionException(existing.status(), AuthzPolicyStatus.DISABLED);
        }
        AuthzPolicy disabled = transitionTo(existing, AuthzPolicyStatus.DISABLED);
        AuthzPolicy saved = repository.save(disabled);
        eventPublisher.unpublishPolicy(saved);
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(
                    caller,
                    AuthzPolicyAuditEvent.POLICY_DISABLED,
                    saved.id(),
                    AuthzPolicyAuditSnapshot.of(existing),
                    AuthzPolicyAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    public Optional<AuthzPolicy> findById(String environmentId, String id) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        AuthzValidators.requireNonBlank(id, "id");
        return repository.findById(environmentId, id);
    }

    @Override
    public List<AuthzPolicy> findAll(String environmentId) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        return repository.findAll(environmentId);
    }

    @Override
    public List<AuthzPolicy> findByKind(String environmentId, AuthzPolicyKind kind) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(kind, "kind");
        return repository.findByKind(environmentId, kind);
    }

    @Override
    public List<AuthzPolicy> findByEntityId(String environmentId, String entityId) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        AuthzValidators.requireNonBlank(entityId, "entityId");
        return repository.findByEntityId(environmentId, entityId);
    }

    @Override
    public PagedResult<AuthzPolicy> findPage(String environmentId, AuthzPolicyFilter filter, Pageable pageable) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(pageable, "pageable must not be null");
        return repository.findPage(environmentId, filter, pageable);
    }

    @Override
    public boolean delete(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        AuthzValidators.requireNonBlank(id, "id");
        Optional<AuthzPolicy> existing = repository.findById(caller.environmentId(), id);
        if (existing.isEmpty()) {
            return false;
        }
        boolean deleted = repository.deleteById(caller.environmentId(), id);
        if (!deleted) {
            return false;
        }
        if (existing.get().status() == AuthzPolicyStatus.DEPLOYED) {
            eventPublisher.unpublishPolicy(existing.get());
        }
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_DELETED, id, AuthzPolicyAuditSnapshot.of(existing.get()), null)
            );
        }
        return true;
    }

    private AuthzPolicy transitionTo(AuthzPolicy existing, AuthzPolicyStatus newStatus) {
        return new AuthzPolicy(
            existing.id(),
            existing.name(),
            existing.kind(),
            existing.entityId(),
            existing.policyText(),
            newStatus,
            existing.environmentId(),
            existing.createdAt(),
            TimeProvider.instantNow()
        );
    }
}
