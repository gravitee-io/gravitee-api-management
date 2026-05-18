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
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditEvent;
import io.gravitee.gamma.authorization.api.PolicyAdminApi;
import io.gravitee.gamma.authorization.api.PolicyAuditSnapshot;
import io.gravitee.gamma.authorization.api.SchemaAdminApi;
import io.gravitee.gamma.authorization.service.exception.InvalidStatusTransitionException;
import io.gravitee.gamma.authorization.service.exception.PolicyNotFoundException;
import io.gravitee.gamma.repository.authorization.api.AuthorizationPolicyRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PolicyServiceImpl implements PolicyAdminApi {

    private final AuthorizationPolicyRepository repository;
    private final EntityIdValidator entityIdValidator;
    private final SchemaAdminApi schemaService;
    private final AuthzEventPublisher eventPublisher;
    private final AuthzAuditPort auditPort;

    public PolicyServiceImpl(
        AuthorizationPolicyRepository repository,
        EntityIdValidator entityIdValidator,
        SchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        this.repository = repository;
        this.entityIdValidator = entityIdValidator;
        this.schemaService = schemaService;
        this.eventPublisher = eventPublisher;
        this.auditPort = auditPort;
    }

    @Override
    @Transactional
    public AuthorizationPolicy create(AuthzCallerContext caller, CreatePolicyCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireMatchingEnv(caller, command.environmentId());
        entityIdValidator.validate(command.kind(), command.entityId());

        Instant now = TimeProvider.instantNow();

        AuthorizationPolicy policy = new AuthorizationPolicy(
            UUID.randomUUID().toString(),
            command.name(),
            command.kind(),
            command.entityId(),
            command.policyText() == null ? "" : command.policyText(),
            AuthorizationPolicyStatus.DRAFT,
            command.environmentId(),
            now,
            now
        );

        AuthorizationPolicy saved = repository.create(policy);
        schemaService.invalidate(saved.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_CREATED, saved.id(), null, PolicyAuditSnapshot.of(saved))
            );
        }
        return saved;
    }

    @Override
    @Transactional
    public AuthorizationPolicy update(AuthzCallerContext caller, String id, UpdatePolicyCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");
        Objects.requireNonNull(command, "command must not be null");

        AuthorizationPolicy existing = repository
            .findByEnvironmentIdAndId(caller.environmentId(), id)
            .orElseThrow(() -> new PolicyNotFoundException(caller.environmentId(), id));

        AuthorizationPolicy updated = new AuthorizationPolicy(
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
        AuthorizationPolicy saved = repository.update(updated);
        if (saved.status() == AuthorizationPolicyStatus.DEPLOYED) {
            eventPublisher.publishPolicyDeployed(saved);
        }
        schemaService.invalidate(saved.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(
                    caller,
                    AuthzPolicyAuditEvent.POLICY_UPDATED,
                    saved.id(),
                    PolicyAuditSnapshot.of(existing),
                    PolicyAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    @Transactional
    public AuthorizationPolicy deploy(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");

        AuthorizationPolicy existing = repository
            .findByEnvironmentIdAndId(caller.environmentId(), id)
            .orElseThrow(() -> new PolicyNotFoundException(caller.environmentId(), id));

        if (existing.status() == AuthorizationPolicyStatus.DEPLOYED) {
            return existing;
        }
        AuthorizationPolicy deployed = transitionTo(existing, AuthorizationPolicyStatus.DEPLOYED);
        AuthorizationPolicy saved = repository.update(deployed);
        eventPublisher.publishPolicyDeployed(saved);
        schemaService.invalidate(saved.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(
                    caller,
                    AuthzPolicyAuditEvent.POLICY_DEPLOYED,
                    saved.id(),
                    PolicyAuditSnapshot.of(existing),
                    PolicyAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    @Transactional
    public AuthorizationPolicy disable(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");

        AuthorizationPolicy existing = repository
            .findByEnvironmentIdAndId(caller.environmentId(), id)
            .orElseThrow(() -> new PolicyNotFoundException(caller.environmentId(), id));

        if (existing.status() == AuthorizationPolicyStatus.DISABLED) {
            return existing;
        }
        if (existing.status() != AuthorizationPolicyStatus.DEPLOYED) {
            throw new InvalidStatusTransitionException(existing.status(), AuthorizationPolicyStatus.DISABLED);
        }
        AuthorizationPolicy disabled = transitionTo(existing, AuthorizationPolicyStatus.DISABLED);
        AuthorizationPolicy saved = repository.update(disabled);
        eventPublisher.unpublishPolicy(saved);
        schemaService.invalidate(saved.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(
                    caller,
                    AuthzPolicyAuditEvent.POLICY_DISABLED,
                    saved.id(),
                    PolicyAuditSnapshot.of(existing),
                    PolicyAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    public Optional<AuthorizationPolicy> findById(String environmentId, String id) {
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(id, "id");
        return repository.findByEnvironmentIdAndId(environmentId, id);
    }

    @Override
    public List<AuthorizationPolicy> findAll(String environmentId) {
        requireNonBlank(environmentId, "environmentId");
        return repository.findAllByEnvironmentId(environmentId);
    }

    @Override
    public List<AuthorizationPolicy> findByKind(String environmentId, AuthorizationPolicyKind kind) {
        requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(kind, "kind");
        return repository.findAllByEnvironmentIdAndKind(environmentId, kind);
    }

    @Override
    public List<AuthorizationPolicy> findByEntityId(String environmentId, String entityId) {
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(entityId, "entityId");
        return repository.findAllByEnvironmentIdAndEntityId(environmentId, entityId);
    }

    @Override
    public PagedResult<AuthorizationPolicy> findPage(String environmentId, PolicyFilter filter, Pageable pageable) {
        requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(pageable, "pageable must not be null");
        PolicyFilter f = filter == null ? PolicyFilter.none() : filter;
        return repository.findPage(environmentId, f.kind(), f.entityId(), f.status(), pageable);
    }

    @Override
    @Transactional
    public boolean delete(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");
        Optional<AuthorizationPolicy> existing = repository.findByEnvironmentIdAndId(caller.environmentId(), id);
        if (existing.isEmpty()) {
            return false;
        }
        boolean deleted = repository.deleteByEnvironmentIdAndId(caller.environmentId(), id) > 0;
        if (!deleted) {
            return false;
        }
        if (existing.get().status() == AuthorizationPolicyStatus.DEPLOYED) {
            eventPublisher.unpublishPolicy(existing.get());
        }
        schemaService.invalidate(caller.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_DELETED, id, PolicyAuditSnapshot.of(existing.get()), null)
            );
        }
        return true;
    }

    private AuthorizationPolicy transitionTo(AuthorizationPolicy existing, AuthorizationPolicyStatus newStatus) {
        return new AuthorizationPolicy(
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

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void requireMatchingEnv(AuthzCallerContext caller, String commandEnvId) {
        requireNonBlank(commandEnvId, "command.environmentId");
        if (!caller.environmentId().equals(commandEnvId)) {
            throw new IllegalArgumentException(
                "command.environmentId (" + commandEnvId + ") does not match caller.environmentId (" + caller.environmentId() + ")"
            );
        }
    }
}
