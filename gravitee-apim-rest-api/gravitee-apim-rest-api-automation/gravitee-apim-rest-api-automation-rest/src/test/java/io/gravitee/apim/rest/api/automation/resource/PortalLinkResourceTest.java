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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalLinkUseCase;
import io.gravitee.apim.rest.api.automation.model.PortalLinkState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalLinkResourceTest extends AbstractResourceTest {

    private static final String PORTAL_HRID = "default-portal";
    private static final String LINK_HRID = "external-docs";

    @Inject
    private GetPortalLinkUseCase getPortalLinkUseCase;

    @Inject
    private DeletePortalLinkUseCase deletePortalLinkUseCase;

    @AfterEach
    void tearDown() {
        reset(getPortalLinkUseCase, deletePortalLinkUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals/" + PORTAL_HRID + "/links";
    }

    @Nested
    class Get {

        @Test
        void should_return_the_link() {
            when(getPortalLinkUseCase.execute(any())).thenReturn(new GetPortalLinkUseCase.Output(aLink()));

            try (var response = rootTarget(LINK_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getPortalLinkUseCase).execute(any(GetPortalLinkUseCase.Input.class));

                var state = response.readEntity(PortalLinkState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(LINK_HRID);
                    soft.assertThat(state.getName()).isEqualTo("External Docs");
                    soft.assertThat(state.getHref()).isEqualTo("https://docs.example.com");
                    soft.assertThat(state.getOrder()).isEqualTo(3);
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                });
            }
        }

        @Test
        void should_return_404_when_link_is_missing() {
            when(getPortalLinkUseCase.execute(any())).thenThrow(new PortalLinkNotFoundException(LINK_HRID));

            try (var response = rootTarget(LINK_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_the_link() {
            try (var response = rootTarget(LINK_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deletePortalLinkUseCase).execute(any(DeletePortalLinkUseCase.Input.class));
            }
        }

        @Test
        void should_return_404_when_link_is_missing() {
            doThrow(new PortalLinkNotFoundException(LINK_HRID)).when(deletePortalLinkUseCase).execute(any());

            try (var response = rootTarget(LINK_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    private static PortalNavigationLink aLink() {
        return PortalNavigationLink.builder()
            .id(PortalNavigationItemId.of("11111111-1111-1111-1111-111111111111"))
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title("External Docs")
            .segment(LINK_HRID)
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("https://docs.example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
