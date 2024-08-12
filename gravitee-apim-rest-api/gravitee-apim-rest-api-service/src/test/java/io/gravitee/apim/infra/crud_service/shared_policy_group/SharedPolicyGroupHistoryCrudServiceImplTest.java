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
package io.gravitee.apim.infra.crud_service.shared_policy_group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.SharedPolicyGroupFixtures;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapter;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapterImpl;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.api.SharedPolicyGroupHistoryRepository;
import io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState;
import java.time.Instant;
import java.util.Date;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SharedPolicyGroupHistoryCrudServiceImplTest {

    private static final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();

    SharedPolicyGroupHistoryRepository repository;
    SharedPolicyGroupAdapter mapper;
    SharedPolicyGroupHistoryCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SharedPolicyGroupHistoryRepository.class);

        mapper = new SharedPolicyGroupAdapterImpl();

        service = new SharedPolicyGroupHistoryCrudServiceImpl(repository, mapper);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_SharedPolicyGroup() {
            // Given
            var sharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            var result = service.create(sharedPolicyGroup);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isNotNull();
                soft.assertThat(result.getName()).isEqualTo(sharedPolicyGroup.getName());
                soft.assertThat(result.getDescription()).isEqualTo(sharedPolicyGroup.getDescription());
                soft.assertThat(result.getSteps()).isEqualTo(sharedPolicyGroup.getSteps());
                soft.assertThat(result.getPhase()).isEqualTo(sharedPolicyGroup.getPhase());
                soft.assertThat(result.getCreatedAt()).isNotNull();
                soft.assertThat(result.getUpdatedAt()).isNotNull();
            });
        }
    }

    private io.gravitee.repository.management.model.SharedPolicyGroup.SharedPolicyGroupBuilder aSharedPolicyGroup() {
        return io.gravitee.repository.management.model.SharedPolicyGroup
            .builder()
            .id("sharedPolicyGroup-id")
            .crossId("cross-id")
            .organizationId("organizationId")
            .environmentId("environmentId")
            .name("sharedPolicyGroup-name")
            .description("sharedPolicyGroup-description")
            .version(1)
            .apiType(ApiType.PROXY)
            .phase(io.gravitee.repository.management.model.SharedPolicyGroup.ExecutionPhase.REQUEST)
            .definition(
                """
                        {
                            "steps": [
                                { "policy": "my-policy", "name": "my-step-1" },
                                { "policy": "my-policy", "name": "my-step-2" }
                            ],
                            "phase": "REQUEST"
                        }
                        """
            )
            .deployedAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .createdAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .lifecycleState(SharedPolicyGroupLifecycleState.DEPLOYED);
    }
}
