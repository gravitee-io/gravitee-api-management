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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.RenderedPageContent;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import java.io.IOException;
import java.util.Map;
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

    @Mock
    private TemplateProcessor templateProcessor;

    private PortalNavigationTemplatingServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new PortalNavigationTemplatingServiceImpl(templateProcessor);
    }

    @Test
    void should_render_template_with_provided_model() throws TemplateProcessorException {
        var model = Map.<String, Object>of("api", "my-api");
        when(templateProcessor.processInlineTemplate(eq("${api}"), argThat(m -> "my-api".equals(m.get("api"))))).thenReturn("my-api");

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(GraviteeMarkdown.of("${api}"), model);

        assertThat(cut.renderGraviteeMarkdown(input)).isEqualTo(RenderedPageContent.of("my-api", PortalPageContentType.GRAVITEE_MARKDOWN));
    }

    @Test
    void should_render_template_with_empty_model() throws TemplateProcessorException {
        when(templateProcessor.processInlineTemplate(eq("plain"), argThat(Map::isEmpty))).thenReturn("plain");

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(GraviteeMarkdown.of("plain"), Map.of());

        assertThat(cut.renderGraviteeMarkdown(input)).isEqualTo(RenderedPageContent.of("plain", PortalPageContentType.GRAVITEE_MARKDOWN));
    }

    @Test
    void should_wrap_freemarker_template_exception_with_blamed_expression_message() {
        var impl = new PortalNavigationTemplatingServiceImpl(new FreemarkerTemplateProcessor());

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(
            GraviteeMarkdown.of("${thisVariableIsMissing}"),
            Map.of()
        );

        assertThatThrownBy(() -> impl.renderGraviteeMarkdown(input))
            .isInstanceOf(PortalPageContentTemplateException.class)
            .hasMessageContaining("Invalid expression or value is missing for thisVariableIsMissing");
    }

    @Test
    void should_wrap_non_template_exception_failure_as_invalid_template() throws TemplateProcessorException {
        when(templateProcessor.processInlineTemplate(anyString(), any())).thenThrow(
            new TemplateProcessorException("tmpl", new IOException("parse failure"))
        );

        var input = new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(GraviteeMarkdown.of("${broken!"), Map.of());

        assertThatThrownBy(() -> cut.renderGraviteeMarkdown(input))
            .isInstanceOf(PortalPageContentTemplateException.class)
            .hasMessageContaining("Invalid template:")
            .hasMessageContaining("parse failure");
    }
}
