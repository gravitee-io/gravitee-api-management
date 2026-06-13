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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_documentation.exception.PortalDocumentationNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalDocumentationUseCase;
import io.gravitee.apim.rest.api.automation.model.DocumentationState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalDocumentationResourceTest extends AbstractResourceTest {

    private static final String PORTAL_HRID = "default-portal";
    private static final String DOC_HRID = "getting-started";

    @Inject
    private GetPortalDocumentationUseCase getPortalDocumentationUseCase;

    @Inject
    private DeletePortalDocumentationUseCase deletePortalDocumentationUseCase;

    @AfterEach
    void tearDown() {
        reset(getPortalDocumentationUseCase, deletePortalDocumentationUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portals/" + PORTAL_HRID + "/documentations";
    }

    @Nested
    class Get {

        @Test
        void should_return_portal_documentation() {
            when(getPortalDocumentationUseCase.execute(any())).thenReturn(new GetPortalDocumentationUseCase.Output(aPageContent()));

            try (var response = rootTarget(DOC_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getPortalDocumentationUseCase).execute(any(GetPortalDocumentationUseCase.Input.class));

                var state = response.readEntity(DocumentationState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(DOC_HRID);
                    soft.assertThat(state.getName()).isEqualTo("Getting Started");
                    soft.assertThat(state.getType().getValue()).isEqualTo("GRAVITEE_MARKDOWN");
                    soft.assertThat(state.getContent()).contains("Hello");
                    soft.assertThat(state.getLocation()).isEqualTo("/projects/alpha");
                    soft.assertThat(state.getOrder()).isEqualTo(1);
                    soft.assertThat(state.getPortalHrid()).isEqualTo(PORTAL_HRID);
                    soft.assertThat(state.getApiHrid()).isNull();
                });
            }
        }

        @Test
        void should_return_404_when_documentation_is_missing() {
            when(getPortalDocumentationUseCase.execute(any())).thenThrow(new PortalDocumentationNotFoundException(DOC_HRID));

            try (var response = rootTarget(DOC_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_portal_documentation() {
            try (var response = rootTarget(DOC_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deletePortalDocumentationUseCase).execute(any(DeletePortalDocumentationUseCase.Input.class));
            }
        }

        @Test
        void should_return_404_when_documentation_is_missing() {
            org.mockito.Mockito.doThrow(new PortalDocumentationNotFoundException(DOC_HRID))
                .when(deletePortalDocumentationUseCase)
                .execute(any());

            try (var response = rootTarget(DOC_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    private static GraviteeMarkdownPageContent aPageContent() {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1"),
            ORGANIZATION,
            ENVIRONMENT,
            GraviteeMarkdown.of("# Hello\n\nMarkdown"),
            new AutomationMetadata(
                AutomationMetadata.ReferenceType.PORTAL,
                "00000000-0000-0000-0000-0000000000a1",
                "Getting Started",
                Optional.of("/projects/alpha"),
                Optional.of(1)
            )
        );
    }
}
