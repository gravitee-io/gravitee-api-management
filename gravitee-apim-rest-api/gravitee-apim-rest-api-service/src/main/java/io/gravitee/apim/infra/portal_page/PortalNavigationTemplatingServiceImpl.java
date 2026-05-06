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
import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortalNavigationTemplatingServiceImpl implements PortalNavigationTemplatingService {

    private final TemplateProcessor templateProcessor;
    private final ApiTemplateService apiTemplateService;
    private final MetadataService metadataService;

    @Override
    public String renderGraviteeMarkdown(RenderPortalNavigationMarkdownInput input) {
        final var executionContext = new ExecutionContext(input.organizationId(), input.environmentId());
        final Map<String, Object> templateParams = new HashMap<>();

        input
            .enclosingApiId()
            .ifPresentOrElse(
                apiId -> {
                    final var genericApiModel = apiTemplateService.findByIdForTemplates(executionContext, apiId, true);
                    templateParams.put("api", genericApiModel);
                },
                () -> {
                    final List<MetadataEntity> metadataList = metadataService.findByReferenceTypeAndReferenceId(
                        MetadataReferenceType.ENVIRONMENT,
                        input.environmentId()
                    );
                    if (metadataList != null) {
                        final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
                        metadataList.forEach(metadata -> mapMetadata.put(metadata.getKey(), metadata.getValue()));
                        templateParams.put("metadata", mapMetadata);
                    }
                }
            );

        try {
            return templateProcessor.processInlineTemplate(input.templateKey(), input.rawMarkdown(), templateParams);
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
