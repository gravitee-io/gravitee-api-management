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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import static java.util.Comparator.comparing;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.core.generic_event.model.PublicationOptions;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianDeploymentNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.GuardianRevision;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * Stores Guardian publications as generic events, the form the gateways read, and reads them back.
 */
@DomainService
@RequiredArgsConstructor
public class GuardianPublicationDomainService {

    // Contract consumed by GuardianGenericEventSubscriber in the gateway handler.
    private static final String GUARDIAN_KIND = "guardian.definition";
    private static final List<EventType> PUBLISH_UNPUBLISH_EVENTS = List.of(
        EventType.PUBLISH_GENERIC_EVENT,
        EventType.UNPUBLISH_GENERIC_EVENT
    );

    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;
    private final GenericEventPublicationDomainService publications;
    private final EventLatestQueryService events;
    private final EventQueryService eventQueryService;
    private final UserCrudService userCrudService;

    /** The latest publication of each Guardian that has one, by Guardian id. */
    public Map<String, GuardianDeployment> findAll(String environmentId, Set<String> guardianIds) {
        if (guardianIds.isEmpty()) {
            return Map.of();
        }
        var ids = guardianIds.stream().collect(toMap(id -> genericEventId(environmentId, id), id -> id));
        return PUBLISH_UNPUBLISH_EVENTS.stream()
            .flatMap(type -> events.findLatestByEntityIds(ids.keySet(), type, Event.EventProperties.GENERIC_EVENT_ID).stream())
            .map(event -> {
                var id = ids.get(event.getProperties().get(Event.EventProperties.GENERIC_EVENT_ID));
                if (id == null) {
                    throw new TechnicalDomainException("Unexpected Guardian publication returned by the event store");
                }
                // the latest event keeps the id of the history event it copies
                return Map.entry(id, read(event, event.getProperties().get(Event.EventProperties.ID), environmentId, id));
            })
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, maxBy(comparing(GuardianDeployment::at))));
    }

    /** The publications of the Guardian, newest first; its withdrawals are left out. */
    public Page<GuardianRevision> revisions(String environmentId, String guardianId, int pageNumber, int pageSize) {
        var response = publicationEvents(environmentId, guardianId, pageNumber, pageSize);
        var deployers = deployers(response.events().stream().map(GuardianPublicationDomainService::deployerId).toList());
        // a row needs the date of the envelope, not the definition it carries
        var revisions = response
            .events()
            .stream()
            .map(event -> {
                var envelope = envelope(event, environmentId, guardianId);
                var at = at(envelope, guardianId);
                return revision(event, at, deployers);
            })
            .toList();
        return new Page<>(revisions, pageNumber, revisions.size(), response.total());
    }

    /**
     * One publication of the Guardian with the definition it deployed.
     *
     * @throws GuardianDeploymentNotFoundException for an unknown id, a withdrawal or a publication of another Guardian
     */
    public GuardianRevision.Deployed revision(String environmentId, String guardianId, String revisionId) {
        var event = eventQueryService
            .findByIdForEnvironment(revisionId, environmentId)
            .filter(found -> found.getType() == EventType.PUBLISH_GENERIC_EVENT)
            .filter(found ->
                genericEventId(environmentId, guardianId).equals(found.getProperties().get(Event.EventProperties.GENERIC_EVENT_ID))
            )
            .orElseThrow(() -> new GuardianDeploymentNotFoundException(revisionId));
        var envelope = envelope(event, environmentId, guardianId);
        var deployers = deployers(Optional.ofNullable(deployerId(event)).stream().toList());
        return new GuardianRevision.Deployed(revision(event, at(envelope, guardianId), deployers), definition(envelope, guardianId));
    }

    public GuardianDeployment.Published publish(
        io.gravitee.definition.model.v4.guardian.Guardian definition,
        String label,
        AuditInfo auditInfo
    ) {
        try {
            var event = GenericEvent.builder()
                .kind(GUARDIAN_KIND)
                .id(definition.getId())
                .payload(jsonSerializer.serialize(definition))
                .build();
            var publication = publications.publish(event, PublicationOptions.withHistory(label), auditInfo);
            return new GuardianDeployment.Published(definition, publication.genericEvent().updatedAt(), publication.eventId());
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to serialize Guardian '" + definition.getId() + "' for deployment", e);
        }
    }

    /** Withdraws the Guardian, keeping with the withdrawal the date of the publication it ends. */
    public GuardianDeployment.Withdrawn withdraw(String guardianId, GuardianDeployment.Published ended, AuditInfo auditInfo) {
        try {
            var event = GenericEvent.builder()
                .kind(GUARDIAN_KIND)
                .id(guardianId)
                .payload(jsonSerializer.serialize(new WithdrawalPayload(ended.at())))
                .build();
            var withdrawal = publications.withdraw(event, PublicationOptions.withHistory(), auditInfo);
            return new GuardianDeployment.Withdrawn(ended.at(), withdrawal.genericEvent().updatedAt());
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to serialize Guardian withdrawal '" + guardianId + "'", e);
        }
    }

    private static String genericEventId(String environmentId, String guardianId) {
        return GenericEvent.latestEventId(environmentId, GUARDIAN_KIND, guardianId);
    }

    // Both stores answer this search by update date, newest first.
    private EventQueryService.SearchResponse publicationEvents(String environmentId, String guardianId, int pageNumber, int pageSize) {
        return eventQueryService.search(
            new EventQueryService.SearchQuery(
                environmentId,
                Optional.empty(),
                Optional.empty(),
                List.of(EventType.PUBLISH_GENERIC_EVENT),
                Map.of(Event.EventProperties.GENERIC_EVENT_ID, genericEventId(environmentId, guardianId)),
                Optional.empty(),
                Optional.empty()
            ),
            new PageableImpl(pageNumber, pageSize)
        );
    }

    private static String deployerId(Event event) {
        return event.getProperties().get(Event.EventProperties.USER);
    }

    private Map<String, BaseUserEntity> deployers(Collection<String> ids) {
        var known = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (known.isEmpty()) {
            return Map.of();
        }
        return userCrudService.findBaseUsersByIds(known).stream().collect(toMap(BaseUserEntity::getId, Function.identity()));
    }

    private static GuardianRevision revision(Event event, Instant at, Map<String, BaseUserEntity> deployers) {
        var deployerId = deployerId(event);
        // a deleted user still shows by id rather than vanishing from the history
        var deployer = deployerId == null ? null : deployers.getOrDefault(deployerId, BaseUserEntity.builder().id(deployerId).build());
        return new GuardianRevision(event.getId(), at, event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL), deployer);
    }

    private GuardianDeployment read(Event event, String revisionId, String environmentId, String guardianId) {
        var envelope = envelope(event, environmentId, guardianId);
        var at = at(envelope, guardianId);
        return event.getType() == EventType.UNPUBLISH_GENERIC_EVENT
            ? readWithdrawal(envelope, at, guardianId)
            : new GuardianDeployment.Published(definition(envelope, guardianId), at, revisionId);
    }

    private JsonNode envelope(Event event, String environmentId, String guardianId) {
        JsonNode envelope;
        try {
            envelope = jsonDeserializer.deserialize(event.getPayload(), JsonNode.class);
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to read the publication of Guardian '" + guardianId + "'", e);
        }
        if (
            envelope == null ||
            !GUARDIAN_KIND.equals(envelope.path("kind").asText()) ||
            !guardianId.equals(envelope.path("id").asText()) ||
            !environmentId.equals(envelope.path("environmentId").asText())
        ) {
            throw new TechnicalDomainException("Invalid publication for Guardian '" + guardianId + "'");
        }
        return envelope;
    }

    private static Instant at(JsonNode envelope, String guardianId) {
        try {
            return Instant.parse(envelope.path("updatedAt").asText());
        } catch (DateTimeParseException e) {
            throw new TechnicalDomainException("Failed to read the publication of Guardian '" + guardianId + "'", e);
        }
    }

    private io.gravitee.definition.model.v4.guardian.Guardian definition(JsonNode envelope, String guardianId) {
        if (!envelope.hasNonNull("payload")) {
            throw new TechnicalDomainException("Missing published definition for Guardian '" + guardianId + "'");
        }
        try {
            return jsonDeserializer.deserialize(
                envelope.get("payload").toString(),
                io.gravitee.definition.model.v4.guardian.Guardian.class
            );
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to read the publication of Guardian '" + guardianId + "'", e);
        }
    }

    private GuardianDeployment.Withdrawn readWithdrawal(JsonNode envelope, Instant at, String guardianId) {
        if (!envelope.hasNonNull("payload")) {
            return new GuardianDeployment.Withdrawn(null, at);
        }
        try {
            // non-strict: a withdrawal written before the payload was reduced to this date carries other fields
            var payload = jsonDeserializer.deserializeNonStrict(envelope.get("payload").toString(), WithdrawalPayload.class);
            return new GuardianDeployment.Withdrawn(payload.deployedAt(), at);
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to read the publication of Guardian '" + guardianId + "'", e);
        }
    }

    // Only what the management side reads back: the gateways ignore a withdrawal's payload.
    private record WithdrawalPayload(Instant deployedAt) {}
}
