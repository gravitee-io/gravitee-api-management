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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdateApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.DeleteApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidateApiLinkUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.PortalLinkSpec;
import io.gravitee.apim.rest.api.automation.model.PortalLinkState;
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
class ApiLinkResourceTest extends AbstractResourceTest {

    private static final String API_HRID = "my-api";
    private static final String LINK_HRID = "external-docs";

    @Inject
    private CreateOrUpdateApiLinkUseCase createOrUpdateApiLinkUseCase;

    @Inject
    private ValidateApiLinkUseCase validateApiLinkUseCase;

    @Inject
    private GetApiLinkUseCase getApiLinkUseCase;

    @Inject
    private DeleteApiLinkUseCase deleteApiLinkUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdateApiLinkUseCase, validateApiLinkUseCase, getApiLinkUseCase, deleteApiLinkUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/links";
    }

    @Nested
    class Run {

        @Test
        void should_apply_and_return_the_api_link_state() {
            when(createOrUpdateApiLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdateApiLinkUseCase.Output(aPersistedLink("External Docs"), List.of())
            );

            try (
                var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("api-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdateApiLinkUseCase).execute(any(CreateOrUpdateApiLinkUseCase.Input.class));

                var state = response.readEntity(PortalLinkState.class);
                assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                assertThat(state.getPortalHrid()).isNull();
            }
        }

        @Test
        void should_build_the_response_from_the_persisted_link_not_from_the_submitted_spec() {
            when(createOrUpdateApiLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdateApiLinkUseCase.Output(aPersistedLink("Normalized Name"), List.of())
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(aLinkSpec("  Normalized Name  ")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.readEntity(PortalLinkState.class).getName()).isEqualTo("Normalized Name");
            }
        }

        @Test
        void should_return_400_with_severe_errors_when_apply_fails_validation() {
            when(createOrUpdateApiLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdateApiLinkUseCase.Output(null, List.of(Validator.Error.severe("href must be a well-formed absolute URL")))
            );

            try (
                var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("api-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);

                var state = response.readEntity(PortalLinkState.class);
                assertThat(state.getErrors()).isNotNull();
                assertThat(state.getErrors().getSevere()).hasSize(1);
                assertThat(state.getErrors().getSevere().get(0)).contains("href");
            }
        }
    }

    @Nested
    class DryRun {

        @Test
        void should_return_populated_state_when_validation_passes() {
            when(validateApiLinkUseCase.execute(any())).thenReturn(new CreateOrUpdateApiLinkUseCase.Output(null, List.of()));

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(validateApiLinkUseCase).execute(any(CreateOrUpdateApiLinkUseCase.Input.class));
                verifyNoInteractions(createOrUpdateApiLinkUseCase);

                var state = response.readEntity(PortalLinkState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(LINK_HRID);
                    soft.assertThat(state.getName()).isEqualTo("External Docs");
                    soft.assertThat(state.getHref()).isEqualTo("https://docs.example.com");
                    soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                    soft.assertThat(state.getPortalHrid()).isNull();
                    soft.assertThat(state.getErrors()).isNull();
                });
            }
        }

        @Test
        void should_return_state_with_errors_when_validation_fails() {
            when(validateApiLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdateApiLinkUseCase.Output(null, List.of(Validator.Error.severe("href must be a well-formed absolute URL")))
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdateApiLinkUseCase);

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
                    .put(Entity.json(readJSON("invalid-api-link.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(validateApiLinkUseCase);
                verifyNoInteractions(createOrUpdateApiLinkUseCase);
            }
        }
    }

    @Nested
    class Get {

        @Test
        void should_return_the_link() {
            when(getApiLinkUseCase.execute(any())).thenReturn(new GetApiLinkUseCase.Output(aPersistedLink("External Docs")));

            try (var response = rootTarget(LINK_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getApiLinkUseCase).execute(any(GetApiLinkUseCase.Input.class));

                var state = response.readEntity(PortalLinkState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(LINK_HRID);
                    soft.assertThat(state.getName()).isEqualTo("External Docs");
                    soft.assertThat(state.getHref()).isEqualTo("https://docs.example.com");
                    soft.assertThat(state.getOrder()).isEqualTo(3);
                    soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                    soft.assertThat(state.getPortalHrid()).isNull();
                });
            }
        }

        @Test
        void should_return_404_when_the_link_does_not_exist() {
            when(getApiLinkUseCase.execute(any())).thenThrow(new PortalLinkNotFoundException(LINK_HRID));

            try (var response = rootTarget(LINK_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_the_link_and_return_no_content() {
            try (var response = rootTarget(LINK_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deleteApiLinkUseCase).execute(any(DeleteApiLinkUseCase.Input.class));
            }
        }

        @Test
        void should_not_error_when_deleting_the_same_link_twice() {
            try (var first = rootTarget(LINK_HRID).request().delete()) {
                assertThat(first.getStatus()).isEqualTo(204);
            }
            try (var second = rootTarget(LINK_HRID).request().delete()) {
                assertThat(second.getStatus()).isEqualTo(204);
            }

            verify(deleteApiLinkUseCase, times(2)).execute(any(DeleteApiLinkUseCase.Input.class));
        }

        @Test
        void should_return_404_when_the_link_does_not_exist() {
            doThrow(new PortalLinkNotFoundException(LINK_HRID)).when(deleteApiLinkUseCase).execute(any());

            try (var response = rootTarget(LINK_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    private static PortalLinkSpec aLinkSpec(String name) {
        return new PortalLinkSpec().hrid(LINK_HRID).name(name).href("https://docs.example.com");
    }

    private static PortalNavigationLink aPersistedLink(String name) {
        return PortalNavigationLink.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title(name)
            .segment(LINK_HRID)
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("https://docs.example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
