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
package io.gravitee.apim.crud_service.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.crud_service.plan.adapter.BasePlanAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PlanCrudServiceImplTest {

    PlanRepository planRepository;
    ApiRepository apiRepository;

    PlanCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        planRepository = mock(PlanRepository.class);
        apiRepository = mock(ApiRepository.class);

        service = new PlanCrudServiceImpl(planRepository, apiRepository, new BasePlanAdapter(new PlanConverter(new ObjectMapper())));
    }

    @Nested
    class FindById {

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
            var result = service.findById(planId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isInstanceOf(BasePlanEntity.class);
                BasePlanEntity plan = (BasePlanEntity) result;

                soft.assertThat(plan.getApiId()).isEqualTo(apiId);
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
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
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(plan.getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getTags()).isEqualTo(Set.of("tag-1"));
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(plan.isCommentRequired()).isTrue();
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
            var result = service.findById(planId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isInstanceOf(io.gravitee.rest.api.model.BasePlanEntity.class);
                io.gravitee.rest.api.model.BasePlanEntity plan = (io.gravitee.rest.api.model.BasePlanEntity) result;

                soft.assertThat(plan.getApiId()).isEqualTo(apiId);
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(plan.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo(planId);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPaths()).isEqualTo(Map.of("/", List.of()));
                soft.assertThat(plan.getPlanMode()).isEqualTo(PlanMode.STANDARD);
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("API_KEY").configuration("security-definition").build());
                soft.assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getPlanType()).isEqualTo(PlanType.API);
                soft.assertThat(plan.getPlanValidation()).isEqualTo(PlanValidationType.AUTO);
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(plan.getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getTags()).isEqualTo(Set.of("tag-1"));
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(plan.isCommentRequired()).isTrue();
            });
        }

        @Test
        void should_throw_when_no_plan_found() throws TechnicalException {
            // Given
            String planId = "unknown";
            when(planRepository.findById(planId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.findById(planId));

            // Then
            assertThat(throwable).isInstanceOf(PlanNotFoundException.class).hasMessage("Plan [" + planId + "] cannot be found.");
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
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find a plan by id: " + planId);
        }

        @Test
        void should_throw_when_technical_exception_occurs_while_fetching_api() throws TechnicalException {
            // Given
            when(planRepository.findById(any(String.class))).thenReturn(Optional.of(planV4().api("api-id").build()));
            when(apiRepository.findById(any(String.class))).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById("plan-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find an API using its ID: api-id");
        }
    }

    private Plan.PlanBuilder planV4() {
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
