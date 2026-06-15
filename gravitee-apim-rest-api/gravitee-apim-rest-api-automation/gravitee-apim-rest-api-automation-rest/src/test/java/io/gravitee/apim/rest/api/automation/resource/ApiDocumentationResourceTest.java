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
import io.gravitee.apim.core.portal_page.exception.ApiDocumentationNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.DeleteApiDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetApiDocumentationUseCase;
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
class ApiDocumentationResourceTest extends AbstractResourceTest {

    private static final String API_HRID = "pets-api";
    private static final String DOC_HRID = "getting-started";

    @Inject
    private GetApiDocumentationUseCase getApiDocumentationUseCase;

    @Inject
    private DeleteApiDocumentationUseCase deleteApiDocumentationUseCase;

    @AfterEach
    void tearDown() {
        reset(getApiDocumentationUseCase, deleteApiDocumentationUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/documentations";
    }

    @Nested
    class Get {

        @Test
        void should_return_api_documentation() {
            when(getApiDocumentationUseCase.execute(any())).thenReturn(new GetApiDocumentationUseCase.Output(aDocumentation()));

            try (var response = rootTarget(DOC_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getApiDocumentationUseCase).execute(any(GetApiDocumentationUseCase.Input.class));

                var state = response.readEntity(DocumentationState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(DOC_HRID);
                    soft.assertThat(state.getName()).isEqualTo("Getting Started");
                    soft.assertThat(state.getType().getValue()).isEqualTo("GRAVITEE_MARKDOWN");
                    soft.assertThat(state.getContent()).contains("Hello");
                    soft.assertThat(state.getLocation()).isEqualTo("/getting-started");
                    soft.assertThat(state.getOrder()).isEqualTo(1);
                    soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                    soft.assertThat(state.getPortalHrid()).isNull();
                });
            }
        }

        @Test
        void should_return_404_when_documentation_is_missing() {
            when(getApiDocumentationUseCase.execute(any())).thenThrow(new ApiDocumentationNotFoundException(DOC_HRID));

            try (var response = rootTarget(DOC_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_api_documentation() {
            try (var response = rootTarget(DOC_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deleteApiDocumentationUseCase).execute(any(DeleteApiDocumentationUseCase.Input.class));
            }
        }

        @Test
        void should_return_404_when_documentation_is_missing() {
            org.mockito.Mockito.doThrow(new ApiDocumentationNotFoundException(DOC_HRID)).when(deleteApiDocumentationUseCase).execute(any());

            try (var response = rootTarget(DOC_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    private static GraviteeMarkdownPageContent aDocumentation() {
        var meta = new AutomationMetadata(
            AutomationMetadata.ReferenceType.API,
            "00000000-0000-0000-0000-0000000000a1",
            "Getting Started",
            Optional.of("/getting-started"),
            Optional.of(1)
        );
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1"),
            ORGANIZATION,
            ENVIRONMENT,
            GraviteeMarkdown.of("# Hello\n\nMarkdown"),
            meta
        );
    }
}
