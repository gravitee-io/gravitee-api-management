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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.domain_service.ValidateApplicationCRDDomainService;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.core.application.use_case.ImportApplicationCRDUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.ApplicationState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApplicationsResourceTest extends AbstractResourceTest {

    @Inject
    private ImportApplicationCRDUseCase importApplicationCRDUseCase;

    @Inject
    private ValidateApplicationCRDDomainService validateApplicationCRDDomainService;

    @AfterEach
    void tearDown() {
        reset(importApplicationCRDUseCase);
        reset(validateApplicationCRDDomainService);
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(importApplicationCRDUseCase.execute(any(ImportApplicationCRDUseCase.Input.class)))
                .thenReturn(
                    new ImportApplicationCRDUseCase.Output(
                        ApplicationCRDStatus.builder().id("application-id").organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build()
                    )
                );
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("application-with-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("application-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("application-hrid");
            });
        }

        @Test
        void should_return_state_from_legacy_id() {
            when(importApplicationCRDUseCase.execute(any(ImportApplicationCRDUseCase.Input.class)))
                .thenAnswer(call -> {
                    ImportApplicationCRDUseCase.Input input = call.getArgument(0, ImportApplicationCRDUseCase.Input.class);
                    return new ImportApplicationCRDUseCase.Output(
                        ApplicationCRDStatus
                            .builder()
                            .id(input.crd().getHrid())
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .build()
                    );
                });

            var state = expectEntity("application-with-hrid.json", false, true);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("application-hrid");
                soft.assertThat(state.getId()).isEqualTo("application-hrid");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_hrid() {
            when(validateApplicationCRDDomainService.validateAndSanitize(any(ValidateApplicationCRDDomainService.Input.class)))
                .thenAnswer(call -> {
                    var input = (ValidateApplicationCRDDomainService.Input) call.getArgument(0);
                    input.spec().setId(IdBuilder.builder(input.auditInfo(), input.spec().getHrid()).buildId());
                    return Validator.Result.ofValue(input);
                });

            var state = expectEntity("application-with-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isNotNull();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("application-hrid");
            });
        }

        @Test
        void should_return_error_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("application-with-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/applications";
    }

    private ApplicationState expectEntity(String spec) {
        return expectEntity(spec, false, false);
    }

    private ApplicationState expectEntity(String spec, boolean dryRun) {
        return expectEntity(spec, dryRun, false);
    }

    private ApplicationState expectEntity(String spec, boolean dryRun, boolean legacy) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("legacy", legacy)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(ApplicationState.class);
        }
    }
}
