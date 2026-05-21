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
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.api.AuthzEntityAuditEvent;
import io.gravitee.gamma.authorization.api.AuthzEntityAuditSnapshot;
import io.gravitee.gamma.authorization.api.AuthzEntityIdConstants;
import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditEvent;
import io.gravitee.gamma.authorization.api.AuthzPolicyAuditSnapshot;
import io.gravitee.gamma.authorization.api.AuthzPolicyRepository;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.api.AuthzValidators;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.listener.EntityIdExtractor;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.exception.AuthzCascadeTooLargeException;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityNotFoundException;
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

public class AuthzEntityServiceImpl implements AuthzEntityAdminApi {

    private static final String API_PREFIX = EntityIdExtractor.API_PREFIX;
    private static final String MCP_PREFIX = EntityIdExtractor.MCP_PREFIX;
    private static final String AGENT_PREFIX = EntityIdExtractor.AGENT_PREFIX;

    public static final int DEFAULT_CASCADE_HARD_LIMIT = 500;

    static final int TRANSACTION_TIMEOUT_SECONDS = 30;

    private final AuthzEntityRepository entityRepository;
    private final AuthzPolicyRepository policyRepository;
    private final AuthzEntityIdValidator entityIdValidator;
    private final AuthzSchemaAdminApi schemaService;
    private final AuthzEventPublisher eventPublisher;
    private final AuthzAuditPort auditPort;
    private final int cascadeHardLimit;

    public AuthzEntityServiceImpl(
        AuthzEntityRepository entityRepository,
        AuthzPolicyRepository policyRepository,
        AuthzEntityIdValidator entityIdValidator,
        AuthzSchemaAdminApi schemaService,
        AuthzEventPublisher eventPublisher,
        AuthzAuditPort auditPort
    ) {
        this(entityRepository, policyRepository, entityIdValidator, schemaService, eventPublisher, auditPort, DEFAULT_CASCADE_HARD_LIMIT);
    }

    public AuthzEntityServiceImpl(
        AuthzEntityRepository entityRepository,
        AuthzPolicyRepository policyRepository,
        AuthzEntityIdValidator entityIdValidator,
        AuthzSchemaAdminApi schemaService,
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
    public AuthzUpsertResult upsert(AuthzCallerContext caller, CreateOrReplaceAuthzEntityCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        AuthzValidators.requireMatchingEnv(caller, command.environmentId());
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
        return new AuthzUpsertResult(outcome.saved(), outcome.wasNew());
    }

    @Override
    @Transactional
    public List<AuthzUpsertResult> bulkUpsert(AuthzCallerContext caller, List<CreateOrReplaceAuthzEntityCommand> commands) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(commands, "commands must not be null");
        if (commands.isEmpty()) {
            return List.of();
        }
        for (CreateOrReplaceAuthzEntityCommand command : commands) {
            Objects.requireNonNull(command, "command must not be null");
            AuthzValidators.requireMatchingEnv(caller, command.environmentId());
            entityIdValidator.validate(command.kind(), command.entityId());
        }

        String environmentId = caller.environmentId();
        List<String> entityIds = commands.stream().map(CreateOrReplaceAuthzEntityCommand::entityId).toList();
        Map<String, AuthzEntity> existingByEntityId = entityRepository
            .findByEntityIds(environmentId, entityIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(AuthzEntity::entityId, e -> e, (a, b) -> a, LinkedHashMap::new));

        List<AuthzUpsertResult> results = new ArrayList<>(commands.size());
        List<UpsertOutcome> outcomes = new ArrayList<>(commands.size());
        for (CreateOrReplaceAuthzEntityCommand command : commands) {
            UpsertOutcome outcome;
            try {
                outcome = doUpsert(command, existingByEntityId.get(command.entityId()));
            } catch (DuplicateKeyException race) {
                outcome = doUpsert(command, null);
            }
            outcomes.add(outcome);
            results.add(new AuthzUpsertResult(outcome.saved(), outcome.wasNew()));
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

    private UpsertOutcome doUpsert(CreateOrReplaceAuthzEntityCommand command, AuthzEntity preloadedExisting) {
        Optional<AuthzEntity> existing = preloadedExisting != null
            ? Optional.of(preloadedExisting)
            : entityRepository.findByEntityId(command.environmentId(), command.entityId());
        Instant now = TimeProvider.instantNow();

        AuthzEntity toSave = existing
            .map(prev ->
                new AuthzEntity(
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
                new AuthzEntity(
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
        AuthzEntity saved = entityRepository.save(toSave);
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
                outcome.wasNew() ? null : AuthzEntityAuditSnapshot.of(outcome.previous()),
                AuthzEntityAuditSnapshot.of(outcome.saved())
            )
        );
    }

    private record UpsertOutcome(AuthzEntity saved, AuthzEntity previous, boolean wasNew) {}

    @Override
    @Transactional
    public AuthzEntity update(AuthzCallerContext caller, String entityId, UpdateAuthzEntityCommand command) {
        Objects.requireNonNull(caller, "caller must not be null");
        Objects.requireNonNull(command, "command must not be null");
        entityIdValidator.validate(AuthzEntityKind.RESOURCE, entityId);

        AuthzEntity existing = entityRepository
            .findByEntityId(caller.environmentId(), entityId)
            .orElseThrow(() -> new AuthzEntityNotFoundException(caller.environmentId(), entityId));

        AuthzEntity updated = new AuthzEntity(
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
        AuthzEntity saved = entityRepository.save(updated);
        eventPublisher.publishEntityUpserted(saved);
        schemaService.invalidate(caller.environmentId());
        if (!caller.isSystem()) {
            auditPort.record(
                AuthzAuditEntry.entity(
                    caller,
                    AuthzEntityAuditEvent.ENTITY_UPDATED,
                    saved.entityId(),
                    AuthzEntityAuditSnapshot.of(existing),
                    AuthzEntityAuditSnapshot.of(saved)
                )
            );
        }
        return saved;
    }

    @Override
    public Optional<AuthzEntity> findByEntityId(String environmentId, String entityId) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        AuthzValidators.requireNonBlank(entityId, "entityId");
        return entityRepository.findByEntityId(environmentId, entityId);
    }

    @Override
    public Set<String> findApiAliases(String environmentId, String apiId) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        AuthzValidators.requireNonBlank(apiId, "apiId");
        Set<String> ids = new LinkedHashSet<>();
        entityRepository.findByEntityId(environmentId, API_PREFIX + apiId).ifPresent(e -> ids.add(e.entityId()));
        entityRepository.findByEntityIdPrefix(environmentId, API_PREFIX + apiId + ".").forEach(e -> ids.add(e.entityId()));
        entityRepository.findByEntityIdPrefix(environmentId, MCP_PREFIX + apiId + ".").forEach(e -> ids.add(e.entityId()));
        entityRepository.findByEntityIdPrefix(environmentId, AGENT_PREFIX + apiId + ".").forEach(e -> ids.add(e.entityId()));
        return ids;
    }

    @Override
    public List<AuthzEntity> find(String environmentId, AuthzEntityFilter filter) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        validateFilter(filter);
        return entityRepository.findPage(environmentId, filter, Pageable.unbounded()).data();
    }

    @Override
    public PagedResult<AuthzEntity> findPage(String environmentId, AuthzEntityFilter filter, Pageable pageable) {
        AuthzValidators.requireNonBlank(environmentId, "environmentId");
        Objects.requireNonNull(pageable, "pageable must not be null");
        validateFilter(filter);
        return entityRepository.findPage(environmentId, filter, pageable);
    }

    private void validateFilter(AuthzEntityFilter filter) {
        if (filter == null) return;
        if (filter.entityIdPrefix() != null && filter.entityIdPrefix().length() > AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH) {
            throw new IllegalArgumentException(
                "entityIdPrefix must be at most " + AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH + " characters"
            );
        }
    }

    @Override
    @Transactional(timeout = TRANSACTION_TIMEOUT_SECONDS)
    public AuthzCascadeResult delete(AuthzCallerContext caller, String entityId) {
        Objects.requireNonNull(caller, "caller must not be null");
        AuthzValidators.requireNonBlank(entityId, "entityId");
        entityIdValidator.validate(AuthzEntityKind.RESOURCE, entityId);

        String environmentId = caller.environmentId();
        LinkedHashMap<String, AuthzEntity> affectedEntities = computeCascadeEntities(environmentId, entityId);

        LinkedHashMap<String, AuthzPolicy> affectedPolicies = new LinkedHashMap<>();
        for (AuthzPolicy p : policyRepository.findByEntityIds(environmentId, affectedEntities.keySet())) {
            affectedPolicies.putIfAbsent(p.id(), p);
        }

        int totalAffected = affectedEntities.size() + affectedPolicies.size();
        if (totalAffected > cascadeHardLimit) {
            throw new AuthzCascadeTooLargeException(totalAffected, cascadeHardLimit);
        }

        entityRepository.deleteByEntityIds(environmentId, affectedEntities.keySet());
        policyRepository.deleteByIds(environmentId, affectedPolicies.keySet());

        for (AuthzEntity e : affectedEntities.values()) {
            eventPublisher.unpublishEntity(e);
        }
        for (AuthzPolicy p : affectedPolicies.values()) {
            eventPublisher.unpublishPolicy(p);
        }

        if (!affectedEntities.isEmpty() || !affectedPolicies.isEmpty()) {
            schemaService.invalidate(environmentId);
        }

        if (!caller.isSystem()) {
            for (AuthzEntity e : affectedEntities.values()) {
                auditPort.record(
                    AuthzAuditEntry.entity(caller, AuthzEntityAuditEvent.ENTITY_DELETED, e.entityId(), AuthzEntityAuditSnapshot.of(e), null)
                );
            }
            for (AuthzPolicy p : affectedPolicies.values()) {
                auditPort.record(
                    AuthzAuditEntry.policy(caller, AuthzPolicyAuditEvent.POLICY_DELETED, p.id(), AuthzPolicyAuditSnapshot.of(p), null)
                );
            }
        }

        return new AuthzCascadeResult(new ArrayList<>(affectedEntities.keySet()), new ArrayList<>(affectedPolicies.keySet()));
    }

    private LinkedHashMap<String, AuthzEntity> computeCascadeEntities(String environmentId, String entityId) {
        LinkedHashMap<String, AuthzEntity> result = new LinkedHashMap<>();
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
        for (AuthzEntity e : entityRepository.findByAnyEntityIdPrefix(environmentId, prefixes)) {
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
