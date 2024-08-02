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
package io.gravitee.apim.infra.crud_service.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PlanCrudServiceImplTest {

    PlanRepository planRepository;
    ApiRepository apiRepository;

    PlanCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        planRepository = mock(PlanRepository.class);
        apiRepository = mock(ApiRepository.class);

        service = new PlanCrudServiceImpl(planRepository);
    }

    @Nested
    class GetById {

        @Test
        void should_return_v4_plan_and_adapt_it() throws TechnicalException {
            // Given
            var planId = "plan-id";
            var apiId = "api-id";
            when(planRepository.findById(planId))
                .thenAnswer(invocation -> Optional.of(planV4().id(invocation.getArgument(0)).api(apiId).build()));
            when(apiRepository.findById(any(String.class)))
                .thenAnswer(invocation ->
                    Optional.of(Api.builder().id(invocation.getArgument(0)).definitionVersion(DefinitionVersion.V4).build())
                );

            // When
            var plan = service.getById(planId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApiId()).isEqualTo(apiId);
                soft.assertThat(plan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo(planId);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPlanMode()).isEqualTo(PlanMode.STANDARD);
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getPlanType()).isEqualTo(PlanType.API);
                soft.assertThat(plan.getPlanValidation()).isEqualTo(PlanValidationType.AUTO);
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
                soft.assertThat(plan.getPlanDefinitionV4().getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getPlanDefinitionV4().getTags()).isEqualTo(Set.of("tag-1"));
            });
        }

        @Test
        void should_return_v2_plan_and_adapt_it() throws TechnicalException {
            // Given
            var planId = "plan-id";
            var apiId = "api-id";
            when(planRepository.findById(planId))
                .thenAnswer(invocation -> Optional.of(planV2().id(invocation.getArgument(0)).api(apiId).build()));
            when(apiRepository.findById(any(String.class)))
                .thenAnswer(invocation ->
                    Optional.of(Api.builder().id(invocation.getArgument(0)).definitionVersion(DefinitionVersion.V2).build())
                );

            // When
            var plan = service.getById(planId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApiId()).isEqualTo(apiId);
                soft.assertThat(plan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo(planId);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPlanMode()).isEqualTo(PlanMode.STANDARD);
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getPlanType()).isEqualTo(PlanType.API);
                soft.assertThat(plan.getPlanValidation()).isEqualTo(PlanValidationType.AUTO);
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
                soft.assertThat(plan.getPlanDefinitionV2().getPaths()).isEqualTo(Map.of("/", List.of()));
                soft.assertThat(plan.getPlanDefinitionV2().getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getPlanDefinitionV2().getTags()).isEqualTo(Set.of("tag-1"));
            });
        }

        @Test
        void should_throw_when_no_plan_found() throws TechnicalException {
            // Given
            String planId = "unknown";
            when(planRepository.findById(planId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.getById(planId));

            // Then
            assertThat(throwable).isInstanceOf(PlanNotFoundException.class).hasMessage("Plan [" + planId + "] cannot be found.");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String planId = "my-plan";
            when(planRepository.findById(planId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.getById(planId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to get a plan by id: " + planId);
        }
    }

    @Nested
    class FindById {

        @Test
        void should_return_plan_when_found() throws TechnicalException {
            var planId = "plan-id";
            var apiId = "api-id";

            when(planRepository.findById(planId))
                .thenAnswer(invocation -> Optional.of(planV4().id(invocation.getArgument(0)).api(apiId).build()));
            var foundPlan = service.findById(planId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(foundPlan).isNotNull();
                soft.assertThat(foundPlan).isPresent();
                var plan = foundPlan.get();

                soft.assertThat(plan.getApiId()).isEqualTo(apiId);
                soft.assertThat(plan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo(planId);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPlanMode()).isEqualTo(PlanMode.STANDARD);
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getPlanType()).isEqualTo(PlanType.API);
                soft.assertThat(plan.getPlanValidation()).isEqualTo(PlanValidationType.AUTO);
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
                soft.assertThat(plan.getPlanDefinitionV4().getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getPlanDefinitionV4().getTags()).isEqualTo(Set.of("tag-1"));
            });
        }

        @Test
        void should_return_empty_when_plan_not_found() throws TechnicalException {
            var planId = "plan-id";

            when(planRepository.findById(planId)).thenAnswer(invocation -> Optional.empty());

            var foundPlan = service.findById(planId);

            Assertions.assertThat(foundPlan).isNotNull().isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String planId = "my-plan";
            when(planRepository.findById(planId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById(planId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find a plan by id: " + planId);
        }
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_v4_plan() {
            var plan = PlanFixtures
                .aKeylessV4()
                .toBuilder()
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .commentRequired(true)
                .build();
            when(planRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.create(plan);

            assertThat(result).isEqualTo(plan);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(planRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(PlanFixtures.aPlanV4()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to create the plan: my-plan");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_an_existing_v4_plan() {
            var plan = PlanFixtures
                .aPlanV4()
                .toBuilder()
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .commentRequired(true)
                .characteristics(List.of("characteristic1", "characteristic2"))
                .commentMessage("Comment message")
                .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
                .generalConditions("General conditions")
                .planDefinitionV4(
                    fixtures.definition.PlanFixtures
                        .aKeylessV4()
                        .toBuilder()
                        .security(PlanSecurity.builder().type("key-less").configuration("{\"nice\": \"config\"}").build())
                        .selectionRule("{#request.attribute['selectionRule'] != null}")
                        .tags(Set.of("tag1", "tag2"))
                        .build()
                )
                .build();
            service.update(plan);

            var captor = ArgumentCaptor.forClass(Plan.class);
            verify(planRepository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    Plan
                        .builder()
                        .id("my-plan")
                        .api("my-api")
                        .crossId("my-plan-crossId")
                        .name("My plan")
                        .definitionVersion(DefinitionVersion.V4)
                        .description("Description")
                        .security(Plan.PlanSecurityType.KEY_LESS)
                        .securityDefinition("{\"nice\": \"config\"}")
                        .selectionRule("{#request.attribute['selectionRule'] != null}")
                        .validation(Plan.PlanValidationType.AUTO)
                        .mode(Plan.PlanMode.STANDARD)
                        .order(1)
                        .type(Plan.PlanType.API)
                        .status(Plan.Status.PUBLISHED)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                        .characteristics(List.of("characteristic1", "characteristic2"))
                        .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
                        .commentRequired(true)
                        .commentMessage("Comment message")
                        .generalConditions("General conditions")
                        .tags(Set.of("tag1", "tag2"))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_update_an_existing_v2_plan() {
            var plan = PlanFixtures
                .aPlanV2()
                .toBuilder()
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .commentRequired(true)
                .characteristics(List.of("characteristic1", "characteristic2"))
                .commentMessage("Comment message")
                .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
                .generalConditions("General conditions")
                .planDefinitionV2(
                    fixtures.definition.PlanFixtures
                        .aKeylessV2()
                        .toBuilder()
                        .selectionRule("{#request.attribute['selectionRule'] != null}")
                        .tags(Set.of("tag1", "tag2"))
                        .build()
                )
                .build();
            service.update(plan);

            var captor = ArgumentCaptor.forClass(Plan.class);
            verify(planRepository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    Plan
                        .builder()
                        .id("my-plan")
                        .api("my-api")
                        .crossId("my-plan-crossId")
                        .name("My plan")
                        .definitionVersion(DefinitionVersion.V2)
                        .description("Description")
                        .security(Plan.PlanSecurityType.KEY_LESS)
                        .securityDefinition("{\"nice\": \"config\"}")
                        .selectionRule("{#request.attribute['selectionRule'] != null}")
                        .validation(Plan.PlanValidationType.AUTO)
                        .mode(Plan.PlanMode.STANDARD)
                        .order(1)
                        .type(Plan.PlanType.API)
                        .status(Plan.Status.PUBLISHED)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                        .characteristics(List.of("characteristic1", "characteristic2"))
                        .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
                        .commentRequired(true)
                        .commentMessage("Comment message")
                        .generalConditions("General conditions")
                        .tags(Set.of("tag1", "tag2"))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_plan() {
            when(planRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = PlanFixtures.aKeylessV4();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(planRepository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(PlanFixtures.aPlanV4()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to update the plan: my-plan");
        }
    }

    @Nested
    class Delete {

        @Test
        @SneakyThrows
        void should_call_delete() {
            var planId = "to-delete";
            service.delete(planId);

            verify(planRepository).delete(planId);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            doThrow(TechnicalException.class).when(planRepository).delete(any());

            // When
            Throwable throwable = catchThrowable(() -> service.delete("to-delete"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to delete the plan: to-delete");
        }
    }

    private Plan.PlanBuilder planV4() {
        return Plan
            .builder()
            .definitionVersion(DefinitionVersion.V4)
            .crossId("cross-id")
            .name("plan-name")
            .description("plan-description")
            .security(Plan.PlanSecurityType.API_KEY)
            .securityDefinition("security-definition")
            .selectionRule("selection-rule")
            .validation(Plan.PlanValidationType.AUTO)
            .mode(Plan.PlanMode.STANDARD)
            .order(1)
            .type(Plan.PlanType.API)
            .status(Plan.Status.PUBLISHED)
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
            .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
            .characteristics(List.of("characteristic-1"))
            .excludedGroups(List.of("excluded-group-1"))
            .commentRequired(true)
            .commentMessage("comment-message")
            .generalConditions("general-conditions")
            .tags(Set.of("tag-1"));
    }

    private Plan.PlanBuilder planV2() {
        return Plan
            .builder()
            .crossId("cross-id")
            .name("plan-name")
            .description("plan-description")
            .security(Plan.PlanSecurityType.API_KEY)
            .securityDefinition("security-definition")
            .selectionRule("selection-rule")
            .validation(Plan.PlanValidationType.AUTO)
            .mode(Plan.PlanMode.STANDARD)
            .order(1)
            .type(Plan.PlanType.API)
            .status(Plan.Status.PUBLISHED)
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
            .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
            .definition("{\"/\":[]}")
            .characteristics(List.of("characteristic-1"))
            .excludedGroups(List.of("excluded-group-1"))
            .commentRequired(true)
            .commentMessage("comment-message")
            .generalConditions("general-conditions")
            .tags(Set.of("tag-1"));
    }
}
