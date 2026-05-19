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
import io.gravitee.gamma.authorization.api.EntityIdConstants;
import io.gravitee.gamma.authorization.api.EntityRepository;
import io.gravitee.gamma.authorization.api.PolicyAuditSnapshot;
import io.gravitee.gamma.authorization.api.PolicyRepository;
import io.gravitee.gamma.authorization.api.SchemaAdminApi;
import io.gravitee.gamma.authorization.api.Validators;
import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.listener.AuthzEntityIdExtractor;
import io.gravitee.gamma.authorization.service.exception.CascadeTooLargeException;
import io.gravitee.gamma.authorization.service.exception.EntityNotFoundException;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
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

    private final EntityRepository entityRepository;
    private final PolicyRepository policyRepository;
    private final EntityIdValidator entityIdValidator;
    private final SchemaAdminApi schemaService;
    private final AuthzEventPublisher eventPublisher;
    private final AuthzAuditPort auditPort;
    private final int cascadeHardLimit;

    public EntityServiceImpl(
        EntityRepository entityRepository,
        PolicyRepository policyRepository,
        EntityIdValidator entityIdValidator,
        SchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        this(entityRepository, policyRepository, entityIdValidator, schemaService, eventPublisher, auditPort, DEFAULT_CASCADE_HARD_LIMIT);
    }

    public EntityServiceImpl(
        EntityRepository entityRepository,
        PolicyRepository policyRepository,
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
        Validators.requireMatchingEnv(caller, command.environmentId());
        entityIdValidator.validate(command.kind(), command.entityId());

        UpsertOutcome outcome;
        try {
            outcome = doUpsert(command, null);
        } catch (DuplicateKeyException race) {
            outcome = doUpsert(command, null);
        }
        eventPublisher.publishEntityUpserted(outcome.saved());
        schemaService.invalidate(command.environmentId());
        recordUpsertAudit(caller, outcome);
        return new UpsertResult(outcome.saved(), outcome.wasNew());
    }

    @Override
    @Transactional
    public List<UpsertResult> bulkUpsert(AuthzCallerContext caller, List<CreateOrReplaceEntityCommand> commands) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(commands, "commands must not be null");
        if (commands.isEmpty()) {
            return List.of();
        }
        for (CreateOrReplaceEntityCommand command : commands) {
            Objects.requireNonNull(command, "command must not be null");
            Validators.requireMatchingEnv(caller, command.environmentId());
            entityIdValidator.validate(command.kind(), command.entityId());
        }

        String environmentId = caller.environmentId();
        List<String> entityIds = commands.stream().map(CreateOrReplaceEntityCommand::entityId).toList();
        Map<String, Entity> existingByEntityId = entityRepository
            .findByEntityIds(environmentId, entityIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(Entity::entityId, e -> e, (a, b) -> a, LinkedHashMap::new));

        List<UpsertResult> results = new ArrayList<>(commands.size());
        List<UpsertOutcome> outcomes = new ArrayList<>(commands.size());
        for (CreateOrReplaceEntityCommand command : commands) {
            UpsertOutcome outcome;
            try {
                outcome = doUpsert(command, existingByEntityId.get(command.entityId()));
            } catch (DuplicateKeyException race) {
                outcome = doUpsert(command, null);
            }
            outcomes.add(outcome);
            results.add(new UpsertResult(outcome.saved(), outcome.wasNew()));
        }
        for (UpsertOutcome outcome : outcomes) {
            eventPublisher.publishEntityUpserted(outcome.saved());
        }
        schemaService.invalidate(environmentId);
        for (UpsertOutcome outcome : outcomes) {
            recordUpsertAudit(caller, outcome);
        }
        return results;
    }

    private UpsertOutcome doUpsert(CreateOrReplaceEntityCommand command, Entity preloadedExisting) {
        Optional<Entity> existing = preloadedExisting != null
            ? Optional.of(preloadedExisting)
            : entityRepository.findByEntityId(command.environmentId(), command.entityId());
        Instant now = TimeProvider.instantNow();

        Entity toSave = existing
            .map(prev ->
                new Entity(
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
                new Entity(
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
        Entity saved = entityRepository.save(toSave);
        return new UpsertOutcome(saved, existing.orElse(null), existing.isEmpty());
    }

    private void recordUpsertAudit(AuthzCallerContext caller, UpsertOutcome outcome) {
        if (caller.isSystem()) {
            return;
        }
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

    private record UpsertOutcome(Entity saved, Entity previous, boolean wasNew) {}

    @Override
    @Transactional
    public Entity update(AuthzCallerContext caller, String entityId, UpdateEntityCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        entityIdValidator.validate(EntityKind.RESOURCE, entityId);

        Entity existing = entityRepository
            .findByEntityId(caller.environmentId(), entityId)
            .orElseThrow(() -> new EntityNotFoundException(caller.environmentId(), entityId));

        Entity updated = new Entity(
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
        Entity saved = entityRepository.save(updated);
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
    public Optional<Entity> findByEntityId(String environmentId, String entityId) {
        Validators.requireNonBlank(environmentId, "environmentId");
        Validators.requireNonBlank(entityId, "entityId");
        return entityRepository.findByEntityId(environmentId, entityId);
    }

    @Override
    public Set<String> findApiAliases(String environmentId, String apiId) {
        Validators.requireNonBlank(environmentId, "environmentId");
        Validators.requireNonBlank(apiId, "apiId");
        Set<String> ids = new LinkedHashSet<>();
        entityRepository.findByEntityId(environmentId, API_PREFIX + apiId).ifPresent(e -> ids.add(e.entityId()));
        entityRepository.findByEntityIdPrefix(environmentId, API_PREFIX + apiId + ".").forEach(e -> ids.add(e.entityId()));
        entityRepository.findByEntityIdPrefix(environmentId, MCP_PREFIX + apiId + ".").forEach(e -> ids.add(e.entityId()));
        entityRepository.findByEntityIdPrefix(environmentId, AGENT_PREFIX + apiId + ".").forEach(e -> ids.add(e.entityId()));
        return ids;
    }

    @Override
    public List<Entity> find(String environmentId, EntityFilter filter) {
        Validators.requireNonBlank(environmentId, "environmentId");
        validateFilter(filter);
        return entityRepository.findPage(environmentId, filter, Pageable.unbounded()).data();
    }

    @Override
    public PagedResult<Entity> findPage(String environmentId, EntityFilter filter, Pageable pageable) {
        Validators.requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(pageable, "pageable must not be null");
        validateFilter(filter);
        return entityRepository.findPage(environmentId, filter, pageable);
    }

    private void validateFilter(EntityFilter filter) {
        if (filter == null) return;
        if (filter.entityIdPrefix() != null && filter.entityIdPrefix().length() > EntityIdConstants.MAX_ENTITY_ID_LENGTH) {
            throw new IllegalArgumentException("entityIdPrefix must be at most " + EntityIdConstants.MAX_ENTITY_ID_LENGTH + " characters");
        }
    }

    @Override
    @Transactional(timeout = TRANSACTION_TIMEOUT_SECONDS)
    public CascadeResult delete(AuthzCallerContext caller, String entityId) {
        Objects.requireNonNull(caller, "caller must not be null");
        Validators.requireNonBlank(entityId, "entityId");
        entityIdValidator.validate(EntityKind.RESOURCE, entityId);

        String environmentId = caller.environmentId();
        LinkedHashMap<String, Entity> affectedEntities = computeCascadeEntities(environmentId, entityId);

        LinkedHashMap<String, Policy> affectedPolicies = new LinkedHashMap<>();
        for (Policy p : policyRepository.findByEntityIds(environmentId, affectedEntities.keySet())) {
            affectedPolicies.putIfAbsent(p.id(), p);
        }

        int totalAffected = affectedEntities.size() + affectedPolicies.size();
        if (totalAffected > cascadeHardLimit) {
            throw new CascadeTooLargeException(totalAffected, cascadeHardLimit);
        }

        entityRepository.deleteByEntityIds(environmentId, affectedEntities.keySet());
        policyRepository.deleteByIds(environmentId, affectedPolicies.keySet());

        for (Entity e : affectedEntities.values()) {
            eventPublisher.unpublishEntity(e);
        }
        for (Policy p : affectedPolicies.values()) {
            eventPublisher.unpublishPolicy(p);
        }

        if (!affectedEntities.isEmpty() || !affectedPolicies.isEmpty()) {
            schemaService.invalidate(environmentId);
        }

        if (!caller.isSystem()) {
            for (Entity e : affectedEntities.values()) {
                auditPort.record(
                    AuthzAuditEntry.entity(caller, AuthzEntityAuditEvent.ENTITY_DELETED, e.entityId(), EntityAuditSnapshot.of(e), null)
                );
            }
            for (Policy p : affectedPolicies.values()) {
                auditPort.record(
                    AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_DELETED, p.id(), PolicyAuditSnapshot.of(p), null)
                );
            }
        }

        return new CascadeResult(new ArrayList<>(affectedEntities.keySet()), new ArrayList<>(affectedPolicies.keySet()));
    }

    private LinkedHashMap<String, Entity> computeCascadeEntities(String environmentId, String entityId) {
        LinkedHashMap<String, Entity> result = new LinkedHashMap<>();
        entityRepository.findByEntityId(environmentId, entityId).ifPresent(e -> result.put(e.entityId(), e));
        List<String> prefixes = new ArrayList<>(3);
        prefixes.add(entityId + ".");
        if (entityId.startsWith(API_PREFIX)) {
            String apiId = entityId.substring(API_PREFIX.length());
            if (!apiId.isEmpty() && !apiId.contains(".")) {
                prefixes.add(MCP_PREFIX + apiId + ".");
                prefixes.add(AGENT_PREFIX + apiId + ".");
            }
        }
        for (Entity e : entityRepository.findByAnyEntityIdPrefix(environmentId, prefixes)) {
            result.putIfAbsent(e.entityId(), e);
        }
        return result;
    }

    private static Map<String, Object> nonNullMap(Map<String, Object> in) {
        return in == null ? Map.of() : in;
    }

    private static List<String> nonNullList(List<String> in) {
        return in == null ? List.of() : in;
    }
}
