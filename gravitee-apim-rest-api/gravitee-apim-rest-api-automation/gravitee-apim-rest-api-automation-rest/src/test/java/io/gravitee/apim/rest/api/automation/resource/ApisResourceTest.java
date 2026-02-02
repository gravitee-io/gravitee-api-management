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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.use_case.ImportApiCRDUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.helpers.SharedPolicyGroupIdHelper;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApisResourceTest extends AbstractResourceTest {

    @Inject
    private ImportApiCRDUseCase importApiCRDUseCase;

    @Inject
    private ValidateApiCRDDomainService validateApiCRDDomainService;

    @AfterEach
    void tearDown() {
        reset(importApiCRDUseCase);
        reset(validateApiCRDDomainService);
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(importApiCRDUseCase.execute(any(ImportApiCRDUseCase.Input.class))).thenReturn(
                new ImportApiCRDUseCase.Output(
                    ApiCRDStatus.builder()
                        .id("api-id")
                        .crossId("api-cross-id")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("api-with-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_legacy_id() {
            when(importApiCRDUseCase.execute(any(ImportApiCRDUseCase.Input.class))).thenAnswer(call -> {
                ImportApiCRDUseCase.Input input = call.getArgument(0, ImportApiCRDUseCase.Input.class);
                return new ImportApiCRDUseCase.Output(
                    ApiCRDStatus.builder()
                        .id(input.spec().getHrid())
                        .crossId("api-cross-id")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                );
            });

            var state = expectEntity("api-with-hrid.json", false, true);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-hrid");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_state_from_cross_id_hrid() {
            var state = expectEntity("api-with-cross-id-and-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_cross_id_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", false)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }

        @Test
        void should_return_state_from_hrid_and_have_spg_hrid_replaced() {
            var state = expectEntity("api-with-hrid-spg-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });

            verify(importApiCRDUseCase, atMostOnce()).execute(
                argThat(
                    input ->
                        input
                            .spec()
                            .getPlans()
                            .values()
                            .stream()
                            .flatMap(p -> p.getFlows().stream())
                            .map(f -> (Flow) f)
                            .flatMap(f ->
                                Stream.concat(
                                    Stream.concat(f.getRequest().stream(), f.getResponse().stream()),
                                    Stream.concat(f.getSubscribe().stream(), f.getPublish().stream())
                                )
                            )
                            .map(Step::getConfiguration)
                            .allMatch(assertion()) &&
                        input
                            .spec()
                            .getFlows()
                            .stream()
                            .map(f -> (Flow) f)
                            .flatMap(f ->
                                Stream.concat(
                                    Stream.concat(f.getRequest().stream(), f.getResponse().stream()),
                                    Stream.concat(f.getSubscribe().stream(), f.getPublish().stream())
                                )
                            )
                            .map(Step::getConfiguration)
                            .allMatch(assertion())
                )
            );
        }

        @Nonnull
        private static Predicate<String> assertion() {
            return s ->
                s.contains(SharedPolicyGroupIdHelper.SHARED_POLICY_GROUP_ID_FIELD) &&
                !s.contains(ApisResource.HRID_FIELD) &&
                !s.contains("test-spg-hrid");
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_hrid() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class))).thenAnswer(call ->
                Validator.Result.ofValue(call.getArgument(0))
            );

            var state = expectEntity("api-with-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isNull();
                soft.assertThat(state.getId()).isNull();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_cross_id() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class))).thenAnswer(call ->
                Validator.Result.ofValue(call.getArgument(0))
            );

            var state = expectEntity("api-with-cross-id-and-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isNull();
                soft.assertThat(state.getId()).isNull();
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_state_from_cross_id_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis";
    }

    private ApiV4State expectEntity(String spec) {
        return expectEntity(spec, false, false);
    }

    private ApiV4State expectEntity(String spec, boolean dryRun) {
        return expectEntity(spec, dryRun, false);
    }

    private ApiV4State expectEntity(String spec, boolean dryRun, boolean legacy) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("legacy", legacy)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(ApiV4State.class);
        }
    }
}
