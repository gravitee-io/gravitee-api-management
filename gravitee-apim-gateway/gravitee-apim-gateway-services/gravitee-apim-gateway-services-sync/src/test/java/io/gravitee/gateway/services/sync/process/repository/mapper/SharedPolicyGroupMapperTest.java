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
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Organization;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private SharedPolicyGroupMapper cut;

    @BeforeEach
    void setUp() {
        cut = new SharedPolicyGroupMapper(objectMapper, new EnvironmentService(environmentRepository, organizationRepository));
    }

    @SneakyThrows
    @Test
    void should_map_shared_policy_group() {
        Organization organization = new Organization();
        organization.setId("orga");
        organization.setHrids(List.of("orga-hrid"));
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        Environment environment = new Environment();
        environment.setId("env");
        environment.setHrids(List.of("env-hrid"));
        environment.setOrganizationId(organization.getId());
        when(environmentRepository.findById("env")).thenReturn(Optional.of(environment));

        Event event = new Event();
        final Date date = new Date();
        event.setPayload(
            objectMapper.writeValueAsString(
                SharedPolicyGroup.builder()
                    .phase(SharedPolicyGroup.Phase.REQUEST)
                    .policies(List.of())
                    .id("spg_id")
                    .name("name")
                    .environmentId("env")
                    .deployedAt(date)
                    .build()
            )
        );
        cut
            .to(event)
            .test()
            .assertValue(reactableSharedPolicyGroup -> {
                assertThat(reactableSharedPolicyGroup.getId()).isEqualTo("spg_id");
                assertThat(reactableSharedPolicyGroup.getName()).isEqualTo("name");
                assertThat(reactableSharedPolicyGroup.getEnvironmentId()).isEqualTo("env");
                assertThat(reactableSharedPolicyGroup.getEnvironmentHrid()).isEqualTo("env-hrid");
                assertThat(reactableSharedPolicyGroup.getOrganizationId()).isEqualTo("orga");
                assertThat(reactableSharedPolicyGroup.getOrganizationHrid()).isEqualTo("orga-hrid");
                assertThat(reactableSharedPolicyGroup.getDeployedAt()).isEqualTo(date);
                assertThat(reactableSharedPolicyGroup.enabled()).isTrue();
                assertThat(reactableSharedPolicyGroup.getDefinition()).satisfies(definition -> {
                    assertThat(definition.getId()).isEqualTo("spg_id");
                    assertThat(definition.getName()).isEqualTo("name");
                    assertThat(definition.getPolicies()).isEmpty();
                    assertThat(definition.getPhase()).isEqualTo(SharedPolicyGroup.Phase.REQUEST);
                });

                return true;
            })
            .assertComplete();
    }
}
