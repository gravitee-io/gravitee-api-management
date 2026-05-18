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
package io.gravitee.apim.authorization.service;

import io.gravitee.apim.authorization.api.AuthzAuditEntry;
import io.gravitee.apim.authorization.api.AuthzAuditPort;
import io.gravitee.apim.authorization.api.AuthzCallerContext;
import io.gravitee.apim.authorization.api.AuthzEventPublisher;
import io.gravitee.apim.authorization.api.AuthzPolicyAuditEvent;
import io.gravitee.apim.authorization.api.PolicyAdminApi;
import io.gravitee.apim.authorization.api.PolicyAuditSnapshot;
import io.gravitee.apim.authorization.api.PolicyRepository;
import io.gravitee.apim.authorization.api.SchemaAdminApi;
import io.gravitee.apim.authorization.domain.Policy;
import io.gravitee.apim.authorization.domain.PolicyKind;
import io.gravitee.apim.authorization.domain.PolicyStatus;
import io.gravitee.apim.authorization.service.exception.InvalidStatusTransitionException;
import io.gravitee.apim.authorization.service.exception.PolicyNotFoundException;
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PolicyServiceImpl implements PolicyAdminApi {

    private final PolicyRepository repository;
    private final EntityIdValidator entityIdValidator;
    private final SchemaAdminApi schemaService;
    private final AuthzEventPublisher eventPublisher;
    private final AuthzAuditPort auditPort;

    public PolicyServiceImpl(
        PolicyRepository repository,
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
    public Policy create(AuthzCallerContext caller, CreatePolicyCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireMatchingEnv(caller, command.environmentId());
        entityIdValidator.validate(command.kind(), command.entityId());

        Instant now = TimeProvider.instantNow();

        Policy policy = new Policy(
            UUID.randomUUID().toString(),
            command.name(),
            command.kind(),
            command.entityId(),
            command.policyText() == null ? "" : command.policyText(),
            PolicyStatus.DRAFT,
            command.environmentId(),
            now,
            now
        );

        Policy saved = repository.save(policy);
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
    public Policy update(AuthzCallerContext caller, String id, UpdatePolicyCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");
        Objects.requireNonNull(command, "command must not be null");

        Policy existing = repository
            .findById(caller.environmentId(), id)
            .orElseThrow(() -> new PolicyNotFoundException(caller.environmentId(), id));

        Policy updated = new Policy(
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
        Policy saved = repository.save(updated);
        if (saved.status() == PolicyStatus.DEPLOYED) {
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
    public Policy deploy(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");

        Policy existing = repository
            .findById(caller.environmentId(), id)
            .orElseThrow(() -> new PolicyNotFoundException(caller.environmentId(), id));

        if (existing.status() == PolicyStatus.DEPLOYED) {
            return existing;
        }
        Policy deployed = transitionTo(existing, PolicyStatus.DEPLOYED);
        Policy saved = repository.save(deployed);
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
    public Policy disable(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");

        Policy existing = repository
            .findById(caller.environmentId(), id)
            .orElseThrow(() -> new PolicyNotFoundException(caller.environmentId(), id));

        if (existing.status() == PolicyStatus.DISABLED) {
            return existing;
        }
        if (existing.status() != PolicyStatus.DEPLOYED) {
            throw new InvalidStatusTransitionException(existing.status(), PolicyStatus.DISABLED);
        }
        Policy disabled = transitionTo(existing, PolicyStatus.DISABLED);
        Policy saved = repository.save(disabled);
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
    public Optional<Policy> findById(String environmentId, String id) {
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(id, "id");
        return repository.findById(environmentId, id);
    }

    @Override
    public List<Policy> findAll(String environmentId) {
        requireNonBlank(environmentId, "environmentId");
        return repository.findAll(environmentId);
    }

    @Override
    public List<Policy> findByKind(String environmentId, PolicyKind kind) {
        requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(kind, "kind");
        return repository.findByKind(environmentId, kind);
    }

    @Override
    public List<Policy> findByEntityId(String environmentId, String entityId) {
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(entityId, "entityId");
        return repository.findByEntityId(environmentId, entityId);
    }

    @Override
    public PagedResult<Policy> findPage(String environmentId, PolicyFilter filter, Pageable pageable) {
        requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(pageable, "pageable must not be null");
        // Delegated to the repo so production stores can short-circuit with a
        // native skip/limit + count plan; the default repo impl falls back
        // to in-memory paging which matches the unpaged finders above.
        return repository.findPage(environmentId, filter == null ? PolicyFilter.none() : filter, pageable);
    }

    @Override
    @Transactional
    public boolean delete(AuthzCallerContext caller, String id) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(id, "id");
        Optional<Policy> existing = repository.findById(caller.environmentId(), id);
        if (existing.isEmpty()) {
            return false;
        }
        boolean deleted = repository.deleteById(caller.environmentId(), id);
        if (!deleted) {
            return false;
        }
        if (existing.get().status() == PolicyStatus.DEPLOYED) {
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

    private Policy transitionTo(Policy existing, PolicyStatus newStatus) {
        return new Policy(
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
