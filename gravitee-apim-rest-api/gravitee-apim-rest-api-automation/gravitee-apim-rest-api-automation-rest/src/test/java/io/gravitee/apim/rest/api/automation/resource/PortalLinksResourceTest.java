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

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdatePortalLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidatePortalLinkUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.PortalLinkState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.HRIDToUUID;
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
class PortalLinksResourceTest extends AbstractResourceTest {

    private static final String PORTAL_HRID = "default-portal";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final PortalNavigationItemId LINK_ID = PortalNavigationItemId.forPortalLink(
        AUDIT_INFO,
        PORTAL_ID.toString(),
        "external-docs"
    );

    @Inject
    private CreateOrUpdatePortalLinkUseCase createOrUpdatePortalLinkUseCase;

    @Inject
    private ValidatePortalLinkUseCase validatePortalLinkUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdatePortalLinkUseCase, validatePortalLinkUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals/" + PORTAL_HRID + "/links";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_populated_state_when_validation_passes() {
            when(validatePortalLinkUseCase.execute(any())).thenReturn(new CreateOrUpdatePortalLinkUseCase.Output(null, List.of()));

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(validatePortalLinkUseCase).execute(any(CreateOrUpdatePortalLinkUseCase.Input.class));
                verifyNoInteractions(createOrUpdatePortalLinkUseCase);

                var state = response.readEntity(PortalLinkState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo("external-docs");
                    soft.assertThat(state.getName()).isEqualTo("External Docs");
                    soft.assertThat(state.getHref()).isEqualTo("https://docs.example.com");
                    soft.assertThat(state.getLocation()).isEqualTo("/projects/alpha");
                    soft.assertThat(state.getOrder()).isEqualTo(1);
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getId()).isEqualTo(LINK_ID.toString());
                    soft.assertThat(state.getErrors()).isNull();
                });
            }
        }

        @Test
        void should_return_state_with_errors_when_validation_fails() {
            when(validatePortalLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalLinkUseCase.Output(null, List.of(Validator.Error.severe("href must be a well-formed absolute URL")))
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalLinkUseCase);

                var state = response.readEntity(PortalLinkState.class);
                assertThat(state.getErrors()).isNotNull();
                assertThat(state.getErrors().getSevere()).hasSize(1);
                assertThat(state.getErrors().getSevere().get(0)).contains("href");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("invalid-portal-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(validatePortalLinkUseCase);
                verifyNoInteractions(createOrUpdatePortalLinkUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_portal_link() {
            when(createOrUpdatePortalLinkUseCase.execute(any())).thenReturn(new CreateOrUpdatePortalLinkUseCase.Output(null, List.of()));

            try (
                var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("portal-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdatePortalLinkUseCase).execute(any(CreateOrUpdatePortalLinkUseCase.Input.class));

                var state = response.readEntity(PortalLinkState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo(LINK_ID.toString());
                    soft.assertThat(state.getHrid()).isEqualTo("external-docs");
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                });
            }
        }

        @Test
        void should_return_400_with_severe_errors_when_apply_fails_validation() {
            when(createOrUpdatePortalLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalLinkUseCase.Output(null, List.of(Validator.Error.severe("href must be a well-formed absolute URL")))
            );

            try (
                var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("portal-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);

                var state = response.readEntity(PortalLinkState.class);
                assertThat(state.getErrors()).isNotNull();
                assertThat(state.getErrors().getSevere()).hasSize(1);
                assertThat(state.getErrors().getSevere().get(0)).contains("href");
            }
        }
    }
}
