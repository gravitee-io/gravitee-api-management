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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SharedPolicyGroupFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapter;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapterImpl;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SharedPolicyGroupCrudServiceImplTest {

    private static final String ENV_ID = SharedPolicyGroupFixtures.aSharedPolicyGroup().getEnvironmentId();

    SharedPolicyGroupRepository repository;
    SharedPolicyGroupAdapter mapper;
    SharedPolicyGroupCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SharedPolicyGroupRepository.class);

        mapper = new SharedPolicyGroupAdapterImpl();

        service = new SharedPolicyGroupCrudServiceImpl(repository, mapper);
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

    @Nested
    class Get {

        @Test
        @SneakyThrows
        void should_return_SharedPolicyGroup_and_adapt_it() {
            // Given
            var sharedPolicyGroupId = "sharedPolicyGroup-id";
            when(repository.findById(sharedPolicyGroupId))
                .thenAnswer(invocation -> Optional.of(aSharedPolicyGroup().id(invocation.getArgument(0)).build()));

            // When
            var result = service.getByEnvironmentId(ENV_ID, sharedPolicyGroupId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isEqualTo(sharedPolicyGroupId);
                soft.assertThat(result.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(result.getOrganizationId()).isEqualTo("organizationId");
                soft.assertThat(result.getEnvironmentId()).isEqualTo("environmentId");
                soft.assertThat(result.getName()).isEqualTo("sharedPolicyGroup-name");
                soft.assertThat(result.getDescription()).isEqualTo("sharedPolicyGroup-description");
                soft.assertThat(result.getVersion()).isEqualTo(1);
                soft.assertThat(result.getApiType()).isEqualTo(ApiType.PROXY);
                soft
                    .assertThat(result.getSteps())
                    .isEqualTo(
                        List.of(
                            Step.builder().policy("my-policy").name("my-step-1").build(),
                            Step.builder().policy("my-policy").name("my-step-2").build()
                        )
                    );
                soft.assertThat(result.getPhase()).isEqualTo(PolicyPlugin.ExecutionPhase.REQUEST);
                soft.assertThat(result.getDeployedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getLifecycleState()).isEqualTo(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED);
            });
        }

        @Test
        @SneakyThrows
        void should_throw_when_no_SharedPolicyGroup_found() {
            // Given
            var sharedPolicyGroupId = "sharedPolicyGroup-id";
            when(repository.findById(sharedPolicyGroupId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.getByEnvironmentId(ENV_ID, sharedPolicyGroupId));

            // Then
            assertThat(throwable)
                .isInstanceOf(SharedPolicyGroupNotFoundException.class)
                .hasMessage("SharedPolicyGroup [" + sharedPolicyGroupId + "] cannot be found.");
        }

        @Test
        @SneakyThrows
        void should_throw_when_environmentId_not_match() {
            // Given
            var sharedPolicyGroupId = "sharedPolicyGroup-id";
            when(repository.findById(sharedPolicyGroupId))
                .thenAnswer(invocation -> Optional.of(aSharedPolicyGroup().id(invocation.getArgument(0)).build()));

            // When
            Throwable throwable = catchThrowable(() -> service.getByEnvironmentId("otherEnvId", sharedPolicyGroupId));

            // Then
            assertThat(throwable)
                .isInstanceOf(SharedPolicyGroupNotFoundException.class)
                .hasMessage("SharedPolicyGroup [" + sharedPolicyGroupId + "] cannot be found.");
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            var sharedPolicyGroupId = "sharedPolicyGroup-id";
            when(repository.findById(sharedPolicyGroupId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.getByEnvironmentId(ENV_ID, sharedPolicyGroupId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find a SharedPolicyGroup with id: " + sharedPolicyGroupId);
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_an_existing_SharedPolicyGroup() {
            SharedPolicyGroup sharedPolicyGroup = SharedPolicyGroupFixtures.aSharedPolicyGroup();
            sharedPolicyGroup.setSteps(
                List.of(
                    Step.builder().policy("my-policy").name("my-step-1").build(),
                    Step.builder().policy("my-policy").name("my-step-2").build()
                )
            );
            service.update(sharedPolicyGroup);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.SharedPolicyGroup.class);
            verify(repository).update(captor.capture());

            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(mapper.fromEntity(sharedPolicyGroup));
            assertThat(captor.getValue().getId()).isEqualTo(sharedPolicyGroup.getId());

            // Expect the definition to be serialized
            assertThat(captor.getValue().getDefinition()).containsSubsequence("my-step-1");
            assertThat(captor.getValue().getDefinition()).containsSubsequence("my-step-2");
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_SharedPolicyGroup() {
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = SharedPolicyGroupFixtures.aSharedPolicyGroup();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(repository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(SharedPolicyGroupFixtures.aSharedPolicyGroup()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to update the SharedPolicyGroup with id: sharedPolicyGroupId");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_membership() throws TechnicalException {
            var sharedPolicyGroupId = "sharedPolicyGroup-id";
            service.delete(sharedPolicyGroupId);
            verify(repository).delete(sharedPolicyGroupId);
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            var sharedPolicyGroupId = "sharedPolicyGroup-id";
            doThrow(new TechnicalException("exception")).when(repository).delete(sharedPolicyGroupId);
            assertThatThrownBy(() -> service.delete(sharedPolicyGroupId))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to delete the SharedPolicyGroup with id: " + sharedPolicyGroupId);
            verify(repository).delete(sharedPolicyGroupId);
        }
    }

    @Nested
    class FindByEnvironmentIdAndCrossId {

        @Test
        @SneakyThrows
        void should_return_SharedPolicyGroup_and_adapt_it() {
            // Given
            var environmentId = "environment-id";
            var crossId = "cross-id";
            when(repository.findByEnvironmentIdAndCrossId(environmentId, crossId))
                .thenAnswer(invocation -> Optional.of(aSharedPolicyGroup().id("sharedPolicyGroup-id").build()));

            // When
            var optional = service.findByEnvironmentIdAndCrossId(environmentId, crossId);
            assertThat(optional).isPresent();

            var result = optional.get();

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isEqualTo("sharedPolicyGroup-id");
                soft.assertThat(result.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(result.getOrganizationId()).isEqualTo("organizationId");
                soft.assertThat(result.getEnvironmentId()).isEqualTo("environmentId");
                soft.assertThat(result.getName()).isEqualTo("sharedPolicyGroup-name");
                soft.assertThat(result.getDescription()).isEqualTo("sharedPolicyGroup-description");
                soft.assertThat(result.getVersion()).isEqualTo(1);
                soft.assertThat(result.getApiType()).isEqualTo(ApiType.PROXY);
                soft
                    .assertThat(result.getSteps())
                    .isEqualTo(
                        List.of(
                            Step.builder().policy("my-policy").name("my-step-1").build(),
                            Step.builder().policy("my-policy").name("my-step-2").build()
                        )
                    );
                soft.assertThat(result.getPhase()).isEqualTo(PolicyPlugin.ExecutionPhase.REQUEST);
                soft.assertThat(result.getDeployedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getLifecycleState()).isEqualTo(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED);
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
