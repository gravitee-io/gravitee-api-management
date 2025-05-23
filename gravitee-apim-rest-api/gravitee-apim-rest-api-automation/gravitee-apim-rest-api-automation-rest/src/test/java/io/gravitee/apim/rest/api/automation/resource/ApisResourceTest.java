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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.use_case.ImportApiCRDUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
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
            when(importApiCRDUseCase.execute(any(ImportApiCRDUseCase.Input.class)))
                .thenReturn(
                    new ImportApiCRDUseCase.Output(
                        ApiCRDStatus
                            .builder()
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
        void should_return_state_from_cross_id() {
            var state = expectEntity("api-with-cross-id-and-no-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isNull();
            });
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_hrid() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class)))
                .thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

            var state = expectEntity("api-with-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isNull();
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_cross_id() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class)))
                .thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));

            var state = expectEntity("api-with-cross-id-and-no-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis";
    }

    private ApiV4State expectEntity(String spec) {
        return expectEntity(spec, false);
    }

    private ApiV4State expectEntity(String spec, boolean dryRun) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            return response.readEntity(ApiV4State.class);
        }
    }
}
