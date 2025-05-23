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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiResourceTest extends AbstractResourceTest {

    @Inject
    private ExportApiCRDUseCase exportApiCRDUseCase;

    @Inject
    private ApiQueryService apiQueryService;

    private static final String API_ID = "api-id";
    private static final String API_CROSS_ID = "api-cross-id";
    private static final String HRID = "api-hrid";

    @AfterEach
    void tearDown() {
        reset(exportApiCRDUseCase);
    }

    @Nested
    class GET {

        @Test
        void should_get_api_from_known_hrid() {
            when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class)))
                .thenReturn(
                    new ExportApiCRDUseCase.Output(
                        ApiCRDSpec
                            .builder()
                            .id(UuidString.generateForEnvironment(ENVIRONMENT, HRID))
                            .crossId(UuidString.generateFrom(ORGANIZATION, HRID))
                            .hrid(HRID)
                            .build()
                    )
                );

            var state = expectEntity(HRID);
            SoftAssertions.assertSoftly(soft -> {
                assertThat(state.getId()).isEqualTo(UuidString.generateForEnvironment(ENVIRONMENT, HRID));
                assertThat(state.getHrid()).isEqualTo(HRID);
                assertThat(state.getCrossId()).isEqualTo(UuidString.generateFrom(ORGANIZATION, HRID));
            });
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class)))
                .thenThrow(new NotFoundException("No API found with hrid: unknown"));

            expectNotFound("unknown");
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        private ApiV4State expectEntity(String hrid) {
            try (var response = rootTarget().path(hrid).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                return response.readEntity(ApiV4State.class);
            }
        }
    }

    @Nested
    class DELETE {

        @Test
        void should_delete_shared_policy_group_and_return_no_content() {
            when(apiQueryService.findByEnvironmentIdAndHRID(any(), eq(HRID))).thenReturn(Optional.of(Api.builder().id(API_ID).build()));

            expectNoContent(HRID);
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            when(apiQueryService.findByEnvironmentIdAndHRID(any(), eq(HRID))).thenReturn(Optional.empty());

            expectNotFound("unknown");
        }

        private void expectNoContent(String hrid) {
            try (var response = rootTarget().path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis";
    }
}
