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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.gateway.reactor.impl.ReactableEvent;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Organization;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DebugMapperTest {

    private DebugMapper cut;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new DebugMapper(new EnvironmentService(environmentRepository, organizationRepository));
    }

    @Test
    void should_map_debug_event() {
        Event event = new Event();
        event.setId("eventId");
        event.setType(EventType.DEBUG_API);
        event.setPayload("{}");
        event.setCreatedAt(new Date());
        event.setEnvironments(Set.of("env"));
        cut
            .to(event)
            .test()
            .assertValue(reactable -> {
                assertThat(reactable).isInstanceOf(ReactableEvent.class);
                ReactableEvent reactableEvent = (ReactableEvent) reactable;
                assertThat(reactableEvent.getId()).isEqualTo(event.getId());
                assertThat(reactableEvent.getDeployedAt()).isEqualTo(event.getCreatedAt());
                assertThat(reactableEvent.getContent()).isEqualTo(event);
                assertThat(reactableEvent.getEnvironmentId()).isNull();
                assertThat(reactableEvent.getEnvironmentHrid()).isNull();
                assertThat(reactableEvent.getOrganizationId()).isNull();
                assertThat(reactableEvent.getOrganizationHrid()).isNull();
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_api_with_env_and_orga() throws TechnicalException {
        Organization organization = new Organization();
        organization.setId("orga");
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        Environment environment = new Environment();
        environment.setId("env");
        environment.setOrganizationId(organization.getId());
        when(environmentRepository.findById("env")).thenReturn(Optional.of(environment));
        Event event = new Event();
        event.setId("eventId");
        event.setType(EventType.DEBUG_API);
        event.setPayload("{}");
        event.setCreatedAt(new Date());
        event.setEnvironments(Set.of("env"));
        cut
            .to(event)
            .test()
            .assertValue(reactable -> {
                assertThat(reactable).isInstanceOf(ReactableEvent.class);
                ReactableEvent reactableEvent = (ReactableEvent) reactable;
                assertThat(reactableEvent.getId()).isEqualTo(event.getId());
                assertThat(reactableEvent.getId()).isEqualTo(event.getId());
                assertThat(reactableEvent.getDeployedAt()).isEqualTo(event.getCreatedAt());
                assertThat(reactableEvent.getContent()).isEqualTo(event);
                assertThat(reactableEvent.getOrganizationId()).isEqualTo(organization.getId());
                assertThat(reactableEvent.getEnvironmentId()).isEqualTo(environment.getId());
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_map_api_with_env_but_ignore_not_found_orga() throws TechnicalException {
        Environment environment = new Environment();
        environment.setId("env");
        environment.setOrganizationId("not found");
        when(environmentRepository.findById("env")).thenReturn(Optional.of(environment));
        Event event = new Event();
        event.setId("eventId");
        event.setType(EventType.DEBUG_API);
        event.setPayload("{}");
        event.setCreatedAt(new Date());
        event.setEnvironments(Set.of("env"));
        cut
            .to(event)
            .test()
            .assertValue(reactable -> {
                assertThat(reactable).isInstanceOf(ReactableEvent.class);
                ReactableEvent reactableEvent = (ReactableEvent) reactable;
                assertThat(reactableEvent.getId()).isEqualTo(event.getId());
                assertThat(reactableEvent.getId()).isEqualTo(event.getId());
                assertThat(reactableEvent.getDeployedAt()).isEqualTo(event.getCreatedAt());
                assertThat(reactableEvent.getContent()).isEqualTo(event);
                assertThat(reactableEvent.getOrganizationId()).isNull();
                assertThat(reactableEvent.getEnvironmentId()).isEqualTo(environment.getId());
                return true;
            })
            .assertComplete();
    }
}
