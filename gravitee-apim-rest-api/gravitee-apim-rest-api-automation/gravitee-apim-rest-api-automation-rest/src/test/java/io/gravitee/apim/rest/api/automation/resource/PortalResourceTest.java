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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.use_case.DeletePortalUseCase;
import io.gravitee.apim.core.portal.use_case.GetPortalUseCase;
import io.gravitee.apim.rest.api.automation.model.PortalState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortalResourceTest extends AbstractResourceTest {

    private static final String HRID = "default-portal";
    private static final PortalId PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000a1");

    @Inject
    private GetPortalUseCase getPortalUseCase;

    @Inject
    private DeletePortalUseCase deletePortalUseCase;

    @AfterEach
    void tearDown() {
        reset(getPortalUseCase, deletePortalUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals";
    }

    @Nested
    class Get {

        @Test
        void should_return_portal_by_hrid() {
            var persisted = Portal.of(PORTAL_ID, ENVIRONMENT, ORGANIZATION, "Default Portal");
            when(getPortalUseCase.execute(any())).thenReturn(new GetPortalUseCase.Output(persisted, List.of()));

            try (var response = rootTarget(HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getPortalUseCase).execute(any(GetPortalUseCase.Input.class));

                var state = response.readEntity(PortalState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo(PORTAL_ID.toString());
                    soft.assertThat(state.getHrid()).isEqualTo(HRID);
                    soft.assertThat(state.getName()).isEqualTo("Default Portal");
                    soft.assertThat(state.getNavigation()).isNullOrEmpty();
                });
            }
        }

        @Test
        void should_return_navigation_alongside_portal() {
            var persisted = Portal.of(PORTAL_ID, ENVIRONMENT, ORGANIZATION, "Default Portal");
            var navigation = List.of(
                new NavigationPath("/projects", Optional.empty()),
                new NavigationPath("/projects/alpha", Optional.of("Alpha"))
            );
            when(getPortalUseCase.execute(any())).thenReturn(new GetPortalUseCase.Output(persisted, navigation));

            try (var response = rootTarget(HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(PortalState.class);
                assertThat(state.getNavigation())
                    .extracting(io.gravitee.apim.rest.api.automation.model.NavigationPath::getPath)
                    .containsExactly("/projects", "/projects/alpha");
                assertThat(state.getNavigation().get(1).getDisplayName()).isEqualTo("Alpha");
            }
        }

        @Test
        void should_return_404_when_missing() {
            when(getPortalUseCase.execute(any())).thenThrow(new PortalNotFoundException(PORTAL_ID.toString()));

            try (var response = rootTarget(HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_portal_by_hrid() {
            try (var response = rootTarget(HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deletePortalUseCase).execute(any(DeletePortalUseCase.Input.class));
            }
        }

        @Test
        void should_return_404_on_delete_missing() {
            org.mockito.Mockito.doThrow(new PortalNotFoundException(PORTAL_ID.toString()))
                .when(deletePortalUseCase)
                .execute(any(DeletePortalUseCase.Input.class));

            try (var response = rootTarget(HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }
}
