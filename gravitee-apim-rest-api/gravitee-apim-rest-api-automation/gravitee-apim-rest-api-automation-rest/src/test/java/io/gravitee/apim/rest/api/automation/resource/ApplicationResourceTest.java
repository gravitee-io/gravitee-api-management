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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.model.ApplicationState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApplicationResourceTest extends AbstractResourceTest {

    @Inject
    private ApplicationService applicationService;

    static final String APPLICATION_ID = "application-id";
    static final String HRID = "application-hrid";
    static final AuditInfo auditInfo = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();

    @AfterEach
    void tearDown() {
        reset(applicationService);
    }

    @Nested
    class GET {

        @Test
        void should_get_application_from_known_hrid() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));
                when(applicationService.findById(any(), any())).thenReturn(
                    ApplicationEntity.builder().id(APPLICATION_ID).hrid(HRID).build()
                );
                var state = expectEntity(HRID);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(APPLICATION_ID);
                    assertThat(state.getHrid()).isEqualTo(HRID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_get_application_from_known_legacy_id() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));
                when(applicationService.findById(any(), any())).thenReturn(
                    ApplicationEntity.builder().id(APPLICATION_ID).hrid(APPLICATION_ID).build()
                );
                var state = expectEntity(APPLICATION_ID, true);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(APPLICATION_ID);
                    assertThat(state.getHrid()).isEqualTo(APPLICATION_ID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            when(applicationService.findById(any(), any())).thenThrow(
                new ApplicationNotFoundException("No Application found with hrid: unknown")
            );

            expectNotFound("unknown");
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        private ApplicationState expectEntity(String hrid) {
            return expectEntity(hrid, false);
        }

        private ApplicationState expectEntity(String hrid, boolean legacy) {
            try (
                var response = rootTarget().queryParam("legacy", legacy).path(hrid).request().accept(MediaType.APPLICATION_JSON_TYPE).get()
            ) {
                return response.readEntity(ApplicationState.class);
            }
        }
    }

    @Nested
    class DELETE {

        @Test
        void should_delete_application_and_return_no_content() {
            expectNoContent(HRID);

            verify(applicationService, atLeastOnce()).archive(any(), eq(IdBuilder.builder(auditInfo, HRID).buildId()));
        }

        @Test
        void should_delete_application_and_return_no_content_with_valid_legacy_id() {
            expectNoContent(APPLICATION_ID, true);

            verify(applicationService, atLeastOnce()).archive(any(), eq(APPLICATION_ID));
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            doThrow(new ApplicationNotFoundException("unknown"))
                .when(applicationService)
                .archive(any(), eq(IdBuilder.builder(auditInfo, "unknown").buildId()));

            expectNotFound("unknown");
        }

        private void expectNoContent(String hrid) {
            expectNoContent(hrid, false);
        }

        private void expectNoContent(String hrid, boolean legacy) {
            try (var response = rootTarget().queryParam("legacy", legacy).path(hrid).request().delete()) {
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
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/applications";
    }
}
