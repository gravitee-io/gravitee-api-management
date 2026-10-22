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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Organization;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private GuardianMapper cut;

    @BeforeEach
    void setUp() {
        cut = new GuardianMapper(objectMapper, new EnvironmentService(environmentRepository, organizationRepository));
    }

    @Test
    void should_extract_guardian_id_from_event_properties() {
        Event event = new Event();
        event.setProperties(Map.of(Event.EventProperties.GUARDIAN_ID.getValue(), "guardian-id"));

        cut.toId(event).test().assertValue("guardian-id").assertComplete();
    }

    @Test
    void should_extract_nothing_when_event_has_no_guardian_id() {
        Event event = new Event();
        event.setId("event-id");

        cut.toId(event).test().assertNoValues().assertComplete();
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(Guardian.Phase.class)
    void should_map_guardian_definition_and_fill_its_environment(Guardian.Phase phase) {
        Organization organization = new Organization();
        organization.setId("orga");
        organization.setHrids(List.of("orga-hrid"));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        Environment environment = new Environment();
        environment.setId("env");
        environment.setHrids(List.of("env-hrid"));
        environment.setOrganizationId(organization.getId());
        when(environmentRepository.findById("env")).thenReturn(Optional.of(environment));

        Date deployedAt = new Date();
        Event event = new Event();
        event.setPayload(
            objectMapper.writeValueAsString(
                Guardian.builder()
                    .id("guardian-id")
                    .name("name")
                    .environmentId("env")
                    .version("1")
                    .phase(phase)
                    .policies(List.of(Step.builder().name("step").policy("a-policy").configuration("{}").build()))
                    .deployedAt(deployedAt)
                    .build()
            )
        );

        cut
            .to(event)
            .test()
            .assertValue(reactableGuardian -> {
                assertThat(reactableGuardian.getId()).isEqualTo("guardian-id");
                assertThat(reactableGuardian.getName()).isEqualTo("name");
                assertThat(reactableGuardian.getEnvironmentId()).isEqualTo("env");
                assertThat(reactableGuardian.getEnvironmentHrid()).isEqualTo("env-hrid");
                assertThat(reactableGuardian.getOrganizationId()).isEqualTo("orga");
                assertThat(reactableGuardian.getOrganizationHrid()).isEqualTo("orga-hrid");
                assertThat(reactableGuardian.getDeployedAt()).isEqualTo(deployedAt);
                assertThat(reactableGuardian.getDefinition()).satisfies(definition -> {
                    assertThat(definition.getId()).isEqualTo("guardian-id");
                    assertThat(definition.getPhase()).isEqualTo(phase);
                    assertThat(definition.getPolicies()).extracting(Step::getPolicy).containsExactly("a-policy");
                });
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_ignore_event_with_unreadable_payload() {
        Event event = new Event();
        event.setId("event-id");
        event.setPayload("not a guardian");

        cut.to(event).test().assertNoValues().assertComplete();
    }
}
