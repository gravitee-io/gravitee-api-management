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
import io.gravitee.gamma.authorization.api.AuthzEntityAuditEvent;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditEvent;
import io.gravitee.gamma.authorization.api.EntityAdminApi;
import io.gravitee.gamma.authorization.api.EntityAuditSnapshot;
import io.gravitee.gamma.authorization.api.PolicyAuditSnapshot;
import io.gravitee.gamma.authorization.api.SchemaAdminApi;
import io.gravitee.gamma.authorization.listener.AuthzEntityIdExtractor;
import io.gravitee.gamma.authorization.service.exception.CascadeTooLargeException;
import io.gravitee.gamma.authorization.service.exception.EntityNotFoundException;
import io.gravitee.gamma.repository.authorization.api.AuthorizationEntityRepository;
import io.gravitee.gamma.repository.authorization.api.AuthorizationPolicyRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

public class EntityServiceImpl implements EntityAdminApi {

    private static final String API_PREFIX = AuthzEntityIdExtractor.API_PREFIX;
    private static final String MCP_PREFIX = AuthzEntityIdExtractor.MCP_PREFIX;
    private static final String AGENT_PREFIX = AuthzEntityIdExtractor.AGENT_PREFIX;

    public static final int DEFAULT_CASCADE_HARD_LIMIT = 500;

    static final int TRANSACTION_TIMEOUT_SECONDS = 30;

    private final AuthorizationEntityRepository entityRepository;
    private final AuthorizationPolicyRepository policyRepository;
    private final EntityIdValidator entityIdValidator;
    private final SchemaAdminApi schemaService;
    private final AuthzEventPublisher eventPublisher;
    private final AuthzAuditPort auditPort;
    private final int cascadeHardLimit;

    public EntityServiceImpl(
        AuthorizationEntityRepository entityRepository,
        AuthorizationPolicyRepository policyRepository,
        EntityIdValidator entityIdValidator,
        SchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        this(entityRepository, policyRepository, entityIdValidator, schemaService, eventPublisher, auditPort, DEFAULT_CASCADE_HARD_LIMIT);
    }

    public EntityServiceImpl(
        AuthorizationEntityRepository entityRepository,
        AuthorizationPolicyRepository policyRepository,
        EntityIdValidator entityIdValidator,
        SchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort,
        int cascadeHardLimit
    ) {
        this.entityRepository = entityRepository;
        this.policyRepository = policyRepository;
        this.entityIdValidator = entityIdValidator;
        this.schemaService = schemaService;
        this.eventPublisher = eventPublisher;
        this.auditPort = auditPort;
        if (cascadeHardLimit < 1) {
            throw new IllegalArgumentException("cascadeHardLimit must be >= 1, got " + cascadeHardLimit);
        }
        this.cascadeHardLimit = cascadeHardLimit;
    }

    @Override
    @Transactional
    public UpsertResult upsert(AuthzCallerContext caller, CreateOrReplaceEntityCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireMatchingEnv(caller, command.environmentId());
        entityIdValidator.validate(command.kind(), command.entityId());

        UpsertOutcome outcome;
        try {
            outcome = doUpsert(command);
        } catch (DuplicateKeyException race) {
            outcome = doUpsert(command);
        }
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.entity(
                    caller,
                    outcome.wasNew() ? AuthzEntityAuditEvent.ENTITY_CREATED : AuthzEntityAuditEvent.ENTITY_UPDATED,
                    outcome.saved().entityId(),
                    outcome.wasNew() ? null : EntityAuditSnapshot.of(outcome.previous()),
                    EntityAuditSnapshot.of(outcome.saved())
                )
            );
        }
        return new UpsertResult(outcome.saved(), outcome.wasNew());
    }

    private UpsertOutcome doUpsert(CreateOrReplaceEntityCommand command) {
        Optional<AuthorizationEntity> existing = entityRepository.findByEnvironmentIdAndEntityId(
            command.environmentId(),
            command.entityId()
        );
        Instant now = TimeProvider.instantNow();

        AuthorizationEntity toSave = existing
            .map(prev ->
                new AuthorizationEntity(
                    prev.id(),
                    command.entityId(),
                    command.kind(),
                    nonNullMap(command.attributes()),
                    nonNullList(command.parents()),
                    command.source(),
                    command.environmentId(),
                    prev.createdAt(),
                    now
                )
            )
            .orElseGet(() ->
                new AuthorizationEntity(
                    UUID.randomUUID().toString(),
                    command.entityId(),
                    command.kind(),
                    nonNullMap(command.attributes()),
                    nonNullList(command.parents()),
                    command.source(),
                    command.environmentId(),
                    now,
                    now
                )
            );
        AuthorizationEntity saved = existing.isPresent() ? entityRepository.update(toSave) : entityRepository.create(toSave);
        eventPublisher.publishEntityUpserted(saved);
        schemaService.invalidate(command.environmentId());
        return new UpsertOutcome(saved, existing.orElse(null), existing.isEmpty());
    }

    private record UpsertOutcome(AuthorizationEntity saved, AuthorizationEntity previous, boolean wasNew) {}

    @Override
    @Transactional
    public AuthorizationEntity update(AuthzCallerContext caller, String entityId, UpdateEntityCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        entityIdValidator.validate(AuthorizationEntityKind.RESOURCE, entityId);

        AuthorizationEntity existing = entityRepository
            .findByEnvironmentIdAndEntityId(caller.environmentId(), entityId)
            .orElseThrow(() -> new EntityNotFoundException(caller.environmentId(), entityId));

        AuthorizationEntity updated = new AuthorizationEntity(
            existing.id(),
            existing.entityId(),
            existing.kind(),
            command.attributes() == null ? existing.attributes() : nonNullMap(command.attributes()),
            command.parents() == null ? existing.parents() : nonNullList(command.parents()),
            existing.source(),
            existing.environmentId(),
            existing.createdAt(),
            TimeProvider.instantNow()
        );
        AuthorizationEntity saved = entityRepository.update(updated);
        eventPublisher.publishEntityUpserted(saved);
        schemaService.invalidate(caller.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.entity(
                    caller,
                    AuthzEntityAuditEvent.ENTITY_UPDATED,
                    saved.entityId(),
                    EntityAuditSnapshot.of(existing),
                    EntityAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    public Optional<AuthorizationEntity> findByEntityId(String environmentId, String entityId) {
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(entityId, "entityId");
        return entityRepository.findByEnvironmentIdAndEntityId(environmentId, entityId);
    }

    @Override
    public Set<String> findApiAliases(String environmentId, String apiId) {
        requireNonBlank(environmentId, "environmentId");
        requireNonBlank(apiId, "apiId");
        Set<String> ids = new LinkedHashSet<>();
        entityRepository.findByEnvironmentIdAndEntityId(environmentId, API_PREFIX + apiId).ifPresent(e -> ids.add(e.entityId()));
        entityRepository
            .findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, API_PREFIX + apiId + ".")
            .forEach(e -> ids.add(e.entityId()));
        entityRepository
            .findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, MCP_PREFIX + apiId + ".")
            .forEach(e -> ids.add(e.entityId()));
        entityRepository
            .findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, AGENT_PREFIX + apiId + ".")
            .forEach(e -> ids.add(e.entityId()));
        return ids;
    }

    @Override
    public List<AuthorizationEntity> find(String environmentId, EntityFilter filter) {
        requireNonBlank(environmentId, "environmentId");
        validateFilter(filter);
        return findInMemory(environmentId, filter);
    }

    @Override
    public PagedResult<AuthorizationEntity> findPage(String environmentId, EntityFilter filter, Pageable pageable) {
        requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(pageable, "pageable must not be null");
        validateFilter(filter);
        // Delegated to the repo so production stores can short-circuit with a
        // native skip/limit + count plan; the default repo impl falls back to
        // in-memory paging which is identical to find() above. Both paths
        // therefore stay consistent for adapters that don't override.
        return PagedResult.of(findInMemory(environmentId, filter), pageable);
    }

    private void validateFilter(EntityFilter filter) {
        if (filter == null) return;
        if (filter.entityIdPrefix() != null && filter.entityIdPrefix().length() > EntityIdValidator.MAX_ENTITY_ID_LENGTH) {
            throw new IllegalArgumentException("entityIdPrefix must be at most " + EntityIdValidator.MAX_ENTITY_ID_LENGTH + " characters");
        }
    }

    private List<AuthorizationEntity> findInMemory(String environmentId, EntityFilter filter) {
        EntityFilter f = filter == null ? EntityFilter.none() : filter;
        List<AuthorizationEntity> base = f.entityIdPrefix() != null
            ? entityRepository.findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, f.entityIdPrefix())
            : entityRepository.findAllByEnvironmentId(environmentId);
        return base
            .stream()
            .filter(e -> f.kind() == null || e.kind() == f.kind())
            .filter(e -> f.source() == null || f.source().equals(e.source()))
            .toList();
    }

    @Override
    @Transactional(timeout = TRANSACTION_TIMEOUT_SECONDS)
    public CascadeResult delete(AuthzCallerContext caller, String entityId) {
        Objects.requireNonNull(caller, "caller must not be null");
        requireNonBlank(entityId, "entityId");
        entityIdValidator.validate(AuthorizationEntityKind.RESOURCE, entityId);

        String environmentId = caller.environmentId();
        LinkedHashMap<String, AuthorizationEntity> affectedEntities = computeCascadeEntities(environmentId, entityId);

        LinkedHashMap<String, AuthorizationPolicy> affectedPolicies = new LinkedHashMap<>();
        for (String eid : affectedEntities.keySet()) {
            for (AuthorizationPolicy p : policyRepository.findAllByEnvironmentIdAndEntityId(environmentId, eid)) {
                affectedPolicies.putIfAbsent(p.id(), p);
            }
        }

        int totalAffected = affectedEntities.size() + affectedPolicies.size();
        if (totalAffected > cascadeHardLimit) {
            throw new CascadeTooLargeException(totalAffected, cascadeHardLimit);
        }

        for (String eid : affectedEntities.keySet()) {
            entityRepository.deleteByEnvironmentIdAndEntityId(environmentId, eid);
        }
        for (String pid : affectedPolicies.keySet()) {
            policyRepository.deleteByEnvironmentIdAndId(environmentId, pid);
        }

        for (AuthorizationEntity e : affectedEntities.values()) {
            eventPublisher.unpublishEntity(e);
        }
        for (AuthorizationPolicy p : affectedPolicies.values()) {
            eventPublisher.unpublishPolicy(p);
        }

        if (!affectedEntities.isEmpty() || !affectedPolicies.isEmpty()) {
            schemaService.invalidate(environmentId);
        }

        if (!caller.isSystem()) {
            for (AuthorizationEntity e : affectedEntities.values()) {
                auditPort.record(
                    AuthzAuditEntry.entity(caller, AuthzEntityAuditEvent.ENTITY_DELETED, e.entityId(), EntityAuditSnapshot.of(e), null)
                );
            }
            for (AuthorizationPolicy p : affectedPolicies.values()) {
                auditPort.record(
                    AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_DELETED, p.id(), PolicyAuditSnapshot.of(p), null)
                );
            }
        }

        return new CascadeResult(new ArrayList<>(affectedEntities.keySet()), new ArrayList<>(affectedPolicies.keySet()));
    }

    private LinkedHashMap<String, AuthorizationEntity> computeCascadeEntities(String environmentId, String entityId) {
        LinkedHashMap<String, AuthorizationEntity> result = new LinkedHashMap<>();
        entityRepository.findByEnvironmentIdAndEntityId(environmentId, entityId).ifPresent(e -> result.put(e.entityId(), e));
        for (AuthorizationEntity e : entityRepository.findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, entityId + ".")) {
            result.putIfAbsent(e.entityId(), e);
        }
        if (entityId.startsWith(API_PREFIX)) {
            String apiId = entityId.substring(API_PREFIX.length());
            if (!apiId.isEmpty() && !apiId.contains(".")) {
                for (AuthorizationEntity e : entityRepository.findAllByEnvironmentIdAndEntityIdStartingWith(
                    environmentId,
                    MCP_PREFIX + apiId + "."
                )) {
                    result.putIfAbsent(e.entityId(), e);
                }
                for (AuthorizationEntity e : entityRepository.findAllByEnvironmentIdAndEntityIdStartingWith(
                    environmentId,
                    AGENT_PREFIX + apiId + "."
                )) {
                    result.putIfAbsent(e.entityId(), e);
                }
            }
        }
        return result;
    }

    private static Map<String, Object> nonNullMap(Map<String, Object> in) {
        return in == null ? Map.of() : in;
    }

    private static List<String> nonNullList(List<String> in) {
        return in == null ? List.of() : in;
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
