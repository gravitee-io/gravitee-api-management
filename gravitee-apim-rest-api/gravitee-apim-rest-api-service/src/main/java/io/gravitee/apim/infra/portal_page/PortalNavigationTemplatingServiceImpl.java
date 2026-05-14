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

import freemarker.template.TemplateException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import io.gravitee.apim.core.portal_page.service_provider.RenderedPageContent;
import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortalNavigationTemplatingServiceImpl implements PortalNavigationTemplatingService {

    private final TemplateProcessor templateProcessor;

    @Override
    public RenderedPageContent renderGraviteeMarkdown(RenderPortalNavigationMarkdownInput input) {
        try {
            return RenderedPageContent.of(templateProcessor.processInlineTemplate(input.rawMarkdown().value(), input.model()));
        } catch (TemplateProcessorException e) {
            // Evaluation failures wrap a freemarker.template.TemplateException; parse failures wrap an IOException
            // (FreeMarker's ParseException extends IOException, not TemplateException).
            if (e.getCause() instanceof TemplateException te) {
                throw new PortalPageContentTemplateException(
                    "Invalid expression or value is missing for " + te.getBlamedExpressionString(),
                    e
                );
            }
            final var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new PortalPageContentTemplateException("Invalid template: " + message, e);
        }
    }
}
