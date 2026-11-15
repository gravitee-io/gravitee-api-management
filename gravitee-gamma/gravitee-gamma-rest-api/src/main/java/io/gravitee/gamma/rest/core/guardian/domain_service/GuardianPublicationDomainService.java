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
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.GuardianRevision;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /** The latest publication of each Guardian that has one, by Guardian id. */
    public Map<String, GuardianDeployment> findAll(String environmentId, Set<String> guardianIds) {
        if (guardianIds.isEmpty()) {
            return Map.of();
        }
        var ids = guardianIds.stream().collect(toMap(id -> GenericEvent.latestEventId(environmentId, GUARDIAN_KIND, id), id -> id));
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
        var revisions = response
            .events()
            .stream()
            .map(event -> revision(event, environmentId, guardianId))
            .toList();
        return new Page<>(revisions, pageNumber, revisions.size(), response.total());
    }

    /** One publication of the Guardian; empty for a withdrawal or a publication of another Guardian. */
    public Optional<GuardianRevision> revision(String environmentId, String guardianId, String revisionId) {
        var genericEventId = GenericEvent.latestEventId(environmentId, GUARDIAN_KIND, guardianId);
        return eventQueryService
            .findByIdForEnvironment(revisionId, environmentId)
            .filter(event -> event.getType() == EventType.PUBLISH_GENERIC_EVENT)
            .filter(event -> genericEventId.equals(event.getProperties().get(Event.EventProperties.GENERIC_EVENT_ID)))
            .map(event -> revision(event, environmentId, guardianId));
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
            return new GuardianDeployment.Published(
                definition,
                publication.updatedAt(),
                publishedRevisionId(auditInfo.environmentId(), definition.getId())
            );
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
            return new GuardianDeployment.Withdrawn(ended.at(), withdrawal.updatedAt());
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to serialize Guardian withdrawal '" + guardianId + "'", e);
        }
    }

    // Both stores answer this search by update date, newest first.
    private EventQueryService.SearchResponse publicationEvents(String environmentId, String guardianId, int pageNumber, int pageSize) {
        return eventQueryService.search(
            new EventQueryService.SearchQuery(
                environmentId,
                Optional.empty(),
                Optional.empty(),
                List.of(EventType.PUBLISH_GENERIC_EVENT),
                Map.of(Event.EventProperties.GENERIC_EVENT_ID, GenericEvent.latestEventId(environmentId, GUARDIAN_KIND, guardianId)),
                Optional.empty(),
                Optional.empty()
            ),
            new PageableImpl(pageNumber, pageSize)
        );
    }

    // The publisher answers the generic event, not the history entry it wrote: the latest event names it.
    private String publishedRevisionId(String environmentId, String guardianId) {
        return events
            .findLatestByEntityIds(
                Set.of(GenericEvent.latestEventId(environmentId, GUARDIAN_KIND, guardianId)),
                EventType.PUBLISH_GENERIC_EVENT,
                Event.EventProperties.GENERIC_EVENT_ID
            )
            .stream()
            .findFirst()
            .map(event -> event.getProperties().get(Event.EventProperties.ID))
            .orElseThrow(() -> new TechnicalDomainException("Missing publication of Guardian '" + guardianId + "' after deploying it"));
    }

    private GuardianRevision revision(Event event, String environmentId, String guardianId) {
        if (!(read(event, event.getId(), environmentId, guardianId) instanceof GuardianDeployment.Published published)) {
            throw new TechnicalDomainException("Unexpected withdrawal in the deployments of Guardian '" + guardianId + "'");
        }
        return new GuardianRevision(
            event.getId(),
            published.at(),
            event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL),
            event.getProperties().get(Event.EventProperties.USER),
            published.definition()
        );
    }

    private GuardianDeployment read(Event event, String revisionId, String environmentId, String guardianId) {
        try {
            JsonNode envelope = jsonDeserializer.deserialize(event.getPayload(), JsonNode.class);
            if (
                envelope == null ||
                !GUARDIAN_KIND.equals(envelope.path("kind").asText()) ||
                !guardianId.equals(envelope.path("id").asText()) ||
                !environmentId.equals(envelope.path("environmentId").asText())
            ) {
                throw new TechnicalDomainException("Invalid publication for Guardian '" + guardianId + "'");
            }
            var at = Instant.parse(envelope.path("updatedAt").asText());
            return event.getType() == EventType.UNPUBLISH_GENERIC_EVENT
                ? readWithdrawal(envelope, at)
                : readPublication(envelope, at, revisionId, guardianId);
        } catch (JsonProcessingException | DateTimeParseException e) {
            throw new TechnicalDomainException("Failed to read the publication of Guardian '" + guardianId + "'", e);
        }
    }

    private GuardianDeployment.Published readPublication(JsonNode envelope, Instant at, String revisionId, String guardianId)
        throws JsonProcessingException {
        if (!envelope.hasNonNull("payload")) {
            throw new TechnicalDomainException("Missing published definition for Guardian '" + guardianId + "'");
        }
        var definition = jsonDeserializer.deserialize(
            envelope.get("payload").toString(),
            io.gravitee.definition.model.v4.guardian.Guardian.class
        );
        return new GuardianDeployment.Published(definition, at, revisionId);
    }

    private GuardianDeployment.Withdrawn readWithdrawal(JsonNode envelope, Instant at) throws JsonProcessingException {
        if (!envelope.hasNonNull("payload")) {
            return new GuardianDeployment.Withdrawn(null, at);
        }
        // non-strict: a withdrawal written before the payload was reduced to this date carries other fields
        var payload = jsonDeserializer.deserializeNonStrict(envelope.get("payload").toString(), WithdrawalPayload.class);
        return new GuardianDeployment.Withdrawn(payload.deployedAt(), at);
    }

    // Only what the management side reads back: the gateways ignore a withdrawal's payload.
    private record WithdrawalPayload(Instant deployedAt) {}
}
