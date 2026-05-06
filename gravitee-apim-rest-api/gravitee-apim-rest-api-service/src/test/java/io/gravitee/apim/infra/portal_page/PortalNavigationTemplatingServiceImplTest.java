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
package io.gravitee.apim.infra.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationTemplatingServiceImplTest {

    private static final String ORG = "org-id";
    private static final String ENV = "env-id";

    @Mock
    private TemplateProcessor templateProcessor;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private MetadataService metadataService;

    private PortalNavigationTemplatingServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new PortalNavigationTemplatingServiceImpl(templateProcessor, apiTemplateService, metadataService);
    }

    @Test
    void should_put_api_in_model_when_enclosing_api_id_is_present() throws TemplateProcessorException {
        var apiModel = org.mockito.Mockito.mock(GenericApiModel.class);
        when(apiTemplateService.findByIdForTemplates(any(ExecutionContext.class), eq("api-1"), eq(true))).thenReturn(apiModel);
        when(templateProcessor.processInlineTemplate(eq("page-key"), eq("${api}"), argThat(m -> m.get("api") == apiModel))).thenReturn(
            "ok"
        );

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(
            "${api}",
            "page-key",
            ORG,
            ENV,
            Optional.of("api-1")
        );

        assertThat(cut.renderGraviteeMarkdown(input)).isEqualTo("ok");
        verify(metadataService, never()).findByReferenceTypeAndReferenceId(any(), anyString());
    }

    @Test
    void should_put_environment_metadata_in_model_when_no_enclosing_api() throws TemplateProcessorException {
        var m = new MetadataEntity();
        m.setKey("k");
        m.setName("n");
        m.setValue("v");
        when(metadataService.findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, ENV)).thenReturn(List.of(m));
        when(
            templateProcessor.processInlineTemplate(
                eq("page-key"),
                eq("${metadata.k}"),
                argThat(model -> "v".equals(((Map<?, ?>) model.get("metadata")).get("k")))
            )
        ).thenReturn("rendered");

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(
            "${metadata.k}",
            "page-key",
            ORG,
            ENV,
            Optional.empty()
        );

        assertThat(cut.renderGraviteeMarkdown(input)).isEqualTo("rendered");
        verify(apiTemplateService, never()).findByIdForTemplates(any(), anyString(), anyBoolean());
    }

    @Test
    void should_not_query_metadata_when_enclosing_api_is_present() throws TemplateProcessorException {
        var apiModel = org.mockito.Mockito.mock(GenericApiModel.class);
        when(apiTemplateService.findByIdForTemplates(any(ExecutionContext.class), eq("api-x"), eq(true))).thenReturn(apiModel);
        when(templateProcessor.processInlineTemplate(anyString(), anyString(), any())).thenReturn("x");

        cut.renderGraviteeMarkdown(
            new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput("md", "key", ORG, ENV, Optional.of("api-x"))
        );

        verify(metadataService, never()).findByReferenceTypeAndReferenceId(any(), anyString());
    }

    @Test
    void should_omit_metadata_when_metadata_list_is_null() throws TemplateProcessorException {
        when(metadataService.findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, ENV)).thenReturn(null);
        when(templateProcessor.processInlineTemplate(eq("key"), eq("plain"), argThat(Map::isEmpty))).thenReturn("plain");

        assertThat(
            cut.renderGraviteeMarkdown(
                new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput("plain", "key", ORG, ENV, Optional.empty())
            )
        ).isEqualTo("plain");
    }

    @Test
    void should_wrap_freemarker_template_exception_with_blamed_expression_message() {
        var freemarkerProcessor = new FreemarkerTemplateProcessor();
        var impl = new PortalNavigationTemplatingServiceImpl(freemarkerProcessor, apiTemplateService, metadataService);
        when(metadataService.findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, ENV)).thenReturn(null);

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(
            "${thisVariableIsMissing}",
            "key",
            ORG,
            ENV,
            Optional.empty()
        );

        assertThatThrownBy(() -> impl.renderGraviteeMarkdown(input))
            .isInstanceOf(PortalPageContentTemplateException.class)
            .hasMessageContaining("Invalid expression or value is missing for thisVariableIsMissing");
    }

    @Test
    void should_wrap_non_template_exception_failure_as_invalid_template() throws TemplateProcessorException {
        when(templateProcessor.processInlineTemplate(anyString(), anyString(), any())).thenThrow(
            new TemplateProcessorException("tmpl", new IOException("parse failure"))
        );

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(
            "${broken!",
            "key",
            ORG,
            ENV,
            Optional.empty()
        );

        assertThatThrownBy(() -> cut.renderGraviteeMarkdown(input))
            .isInstanceOf(PortalPageContentTemplateException.class)
            .hasMessageContaining("Invalid template:")
            .hasMessageContaining("parse failure");
    }
}
