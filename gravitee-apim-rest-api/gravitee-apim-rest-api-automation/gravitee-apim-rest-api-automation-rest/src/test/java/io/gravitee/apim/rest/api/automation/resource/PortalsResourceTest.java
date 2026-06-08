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

import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.use_case.CreateOrUpdatePortalUseCase;
import io.gravitee.apim.rest.api.automation.model.PortalState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortalsResourceTest extends AbstractResourceTest {

    @Inject
    private CreateOrUpdatePortalUseCase createOrUpdatePortalUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdatePortalUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_populated_state_without_calling_use_case() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalUseCase);

                var state = response.readEntity(PortalState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo("default-portal");
                    soft.assertThat(state.getName()).isEqualTo("Default Portal");
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getId()).isNotBlank();
                });
            }
        }

        @Test
        void should_echo_navigation_in_dry_run() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-with-navigation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalUseCase);

                var state = response.readEntity(PortalState.class);
                assertThat(state.getNavigation())
                    .extracting(io.gravitee.apim.rest.api.automation.model.NavigationPath::getPath)
                    .containsExactly("/projects/alpha", "/projects/alpha/docs");
                assertThat(state.getNavigation().get(0).getDisplayName()).isEqualTo("Alpha");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("invalid-portal.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(createOrUpdatePortalUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_portal() {
            var persisted = Portal.of(PortalId.of("00000000-0000-0000-0000-0000000000a1"), ENVIRONMENT, ORGANIZATION, "Default Portal");
            when(createOrUpdatePortalUseCase.execute(any())).thenReturn(new CreateOrUpdatePortalUseCase.Output(persisted, List.of()));

            try (var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("portal.json")))) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdatePortalUseCase).execute(any(CreateOrUpdatePortalUseCase.Input.class));

                var state = response.readEntity(PortalState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo("00000000-0000-0000-0000-0000000000a1");
                    soft.assertThat(state.getHrid()).isEqualTo("default-portal");
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getName()).isEqualTo("Default Portal");
                    soft.assertThat(state.getNavigation()).isNullOrEmpty();
                });
            }
        }

        @Test
        void should_pass_navigation_to_use_case_and_echo_in_response() {
            var persisted = Portal.of(PortalId.of("00000000-0000-0000-0000-0000000000a1"), ENVIRONMENT, ORGANIZATION, "Default Portal");
            var echoed = List.of(
                new NavigationPath("/projects", Optional.empty()),
                new NavigationPath("/projects/alpha", Optional.of("Alpha")),
                new NavigationPath("/projects/alpha/docs", Optional.empty())
            );
            when(createOrUpdatePortalUseCase.execute(any())).thenReturn(new CreateOrUpdatePortalUseCase.Output(persisted, echoed));

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("portal-with-navigation.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);

                var inputCaptor = ArgumentCaptor.forClass(CreateOrUpdatePortalUseCase.Input.class);
                verify(createOrUpdatePortalUseCase).execute(inputCaptor.capture());
                assertThat(inputCaptor.getValue().navigation())
                    .extracting(NavigationPath::path)
                    .containsExactly("/projects/alpha", "/projects/alpha/docs");
                assertThat(inputCaptor.getValue().navigation().get(0).displayName()).isEqualTo(Optional.of("Alpha"));

                var state = response.readEntity(PortalState.class);
                assertThat(state.getNavigation())
                    .extracting(io.gravitee.apim.rest.api.automation.model.NavigationPath::getPath)
                    .containsExactly("/projects", "/projects/alpha", "/projects/alpha/docs");
                assertThat(state.getNavigation().get(1).getDisplayName()).isEqualTo("Alpha");
            }
        }
    }
}
