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
package io.gravitee.gamma.rest.core.guardian;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.core.generic_event.model.PublicationOptions;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianPublicationDomainService;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import io.gravitee.rest.api.model.EventType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The event store Guardian publications go through, kept in memory: tests publish and withdraw
 * through the production writer and read back through the production query, so the gateway
 * envelope is exercised rather than stubbed.
 */
public class GuardianPublicationFixture {

    private static final String GUARDIAN_KIND = "guardian.definition";

    public final EventLatestCrudInMemory latest = new EventLatestCrudInMemory();
    public final EventCrudInMemory history = new EventCrudInMemory() {
        @Override
        public Event createEvent(
            String organizationId,
            String environmentId,
            Set<String> environmentIds,
            EventType eventType,
            Object content,
            Map<Event.EventProperties, String> properties
        ) {
            if (eventType == EventType.PUBLISH_GENERIC_EVENT) {
                beforePublication.run();
            }
            if (eventType == EventType.UNPUBLISH_GENERIC_EVENT) {
                beforeWithdrawal.run();
                if (failWithdrawal) {
                    throw new TechnicalDomainException("Publication store unavailable");
                }
            }
            return super.createEvent(organizationId, environmentId, environmentIds, eventType, content, properties);
        }
    };
    public final EventLatestQueryServiceInMemory query = new EventLatestQueryServiceInMemory() {
        @Override
        public List<Event> findLatestByEntityIds(Set<String> ids, EventType type, Event.EventProperties key) {
            reset();
            initWith(latest.storage());
            return super.findLatestByEntityIds(ids, type, key);
        }
    };
    public final GenericEventWriterDomainService writer = new GenericEventWriterDomainService(
        history,
        latest,
        GraviteeJacksonMapper.getInstance()
    );
    public final GenericEventPublicationDomainService publisher = new GenericEventPublicationDomainService(
        writer,
        GraviteeJacksonMapper.getInstance()
    );
    public boolean failWithdrawal;
    public Runnable beforeWithdrawal = () -> {};
    public Runnable beforePublication = () -> {};

    public GuardianDeploymentDomainService service(GuardianRepository repository) {
        return new GuardianDeploymentDomainService(repository, store(), new JacksonJsonSerializer(), new JacksonJsonDeserializer());
    }

    public GuardianPublicationDomainService store() {
        return new GuardianPublicationDomainService(new JacksonJsonSerializer(), new JacksonJsonDeserializer(), publisher, query);
    }

    public void givenPublication(io.gravitee.definition.model.v4.guardian.Guardian definition, Instant publishedAt) {
        var auditInfo = AuditInfo.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(definition.getEnvironmentId())
            .actor(AuditActor.builder().userId("deployer").build())
            .build();
        write(EventType.PUBLISH_GENERIC_EVENT, definition.getId(), auditInfo, publishedAt, definition);
    }

    /** A withdrawal carrying the date of the publication it ends, or no payload at all when {@code deployedAt} is null. */
    public void givenWithdrawal(io.gravitee.definition.model.v4.guardian.Guardian definition, Instant deployedAt, Instant withdrawnAt) {
        var auditInfo = AuditInfo.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(definition.getEnvironmentId())
            .actor(AuditActor.builder().userId("deployer").build())
            .build();
        write(
            EventType.UNPUBLISH_GENERIC_EVENT,
            definition.getId(),
            auditInfo,
            withdrawnAt,
            deployedAt == null ? null : Map.of("deployedAt", deployedAt.toString())
        );
    }

    public Optional<GuardianDeployment> find(String environmentId, String guardianId) {
        var guardian = Guardian.builder()
            .definition(io.gravitee.definition.model.v4.guardian.Guardian.builder().id(guardianId).environmentId(environmentId).build())
            .build();
        return Optional.ofNullable(service(new InMemoryGuardianRepository()).current(guardian));
    }

    public GuardianDeployment.Published published(String environmentId, String guardianId) {
        return (GuardianDeployment.Published) find(environmentId, guardianId).orElseThrow();
    }

    public boolean withdrawn(String environmentId, String guardianId) {
        return find(environmentId, guardianId).orElseThrow() instanceof GuardianDeployment.Withdrawn;
    }

    public int publications() {
        return history
            .storage()
            .stream()
            .filter(event -> event.getType() == EventType.PUBLISH_GENERIC_EVENT)
            .toList()
            .size();
    }

    public String lastLabel() {
        return latest
            .storage()
            .stream()
            .filter(event -> event.getType() == EventType.PUBLISH_GENERIC_EVENT)
            .findFirst()
            .map(event -> event.getProperties().get(Event.EventProperties.DEPLOYMENT_LABEL))
            .orElse(null);
    }

    public void reset() {
        history.reset();
        latest.reset();
        failWithdrawal = false;
        beforeWithdrawal = () -> {};
        beforePublication = () -> {};
    }

    private void write(EventType type, String guardianId, AuditInfo auditInfo, Instant updatedAt, Object payload) {
        try {
            var event = GenericEvent.builder()
                .kind(GUARDIAN_KIND)
                .id(guardianId)
                .environmentId(auditInfo.environmentId())
                .organizationId(auditInfo.organizationId())
                .updatedAt(updatedAt)
                .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(payload))
                .build();
            writer.write(type, event, auditInfo, PublicationOptions.withHistory());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
