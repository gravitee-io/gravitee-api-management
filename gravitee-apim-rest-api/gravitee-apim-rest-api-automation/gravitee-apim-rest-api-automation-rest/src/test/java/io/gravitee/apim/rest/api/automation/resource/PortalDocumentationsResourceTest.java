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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdatePortalDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidatePortalDocumentationUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.DocumentationState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalDocumentationsResourceTest extends AbstractResourceTest {

    private static final String PORTAL_HRID = "default-portal";

    @Inject
    private CreateOrUpdatePortalDocumentationUseCase createOrUpdatePortalDocumentationUseCase;

    @Inject
    private ValidatePortalDocumentationUseCase validatePortalDocumentationUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdatePortalDocumentationUseCase, validatePortalDocumentationUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals/" + PORTAL_HRID + "/documentations";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_populated_state_when_validation_passes() {
            when(validatePortalDocumentationUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalDocumentationUseCase.Output(
                    PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1"),
                    List.of()
                )
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-documentation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(validatePortalDocumentationUseCase).execute(any(CreateOrUpdatePortalDocumentationUseCase.Input.class));
                verifyNoInteractions(createOrUpdatePortalDocumentationUseCase);

                var state = response.readEntity(DocumentationState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo("getting-started");
                    soft.assertThat(state.getName()).isEqualTo("Getting Started");
                    soft.assertThat(state.getType().getValue()).isEqualTo("GRAVITEE_MARKDOWN");
                    soft.assertThat(state.getContent()).contains("Hello");
                    soft.assertThat(state.getLocation()).isEqualTo("/projects/alpha");
                    soft.assertThat(state.getOrder()).isEqualTo(1);
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getApiHrid()).isNull();
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getId()).isEqualTo("00000000-0000-0000-0000-0000000000c1");
                    soft.assertThat(state.getErrors()).isNull();
                });
            }
        }

        @Test
        void should_return_state_with_errors_when_validation_fails() {
            when(validatePortalDocumentationUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalDocumentationUseCase.Output(
                    PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1"),
                    List.of(Validator.Error.severe("location must start with '/'"))
                )
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-documentation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalDocumentationUseCase);

                var state = response.readEntity(DocumentationState.class);
                assertThat(state.getErrors()).isNotNull();
                assertThat(state.getErrors().getSevere()).hasSize(1);
                assertThat(state.getErrors().getSevere().get(0)).contains("location");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("invalid-portal-documentation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(validatePortalDocumentationUseCase);
                verifyNoInteractions(createOrUpdatePortalDocumentationUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_portal_documentation() {
            when(createOrUpdatePortalDocumentationUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalDocumentationUseCase.Output(
                    PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1"),
                    List.of()
                )
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-documentation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdatePortalDocumentationUseCase).execute(any(CreateOrUpdatePortalDocumentationUseCase.Input.class));

                var state = response.readEntity(DocumentationState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo("00000000-0000-0000-0000-0000000000c1");
                    soft.assertThat(state.getHrid()).isEqualTo("getting-started");
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getApiHrid()).isNull();
                });
            }
        }

        @Test
        void should_return_400_when_validation_severe_error_raised() {
            when(createOrUpdatePortalDocumentationUseCase.execute(any())).thenThrow(
                new ValidationDomainException("location must start with '/'")
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-documentation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }
}
