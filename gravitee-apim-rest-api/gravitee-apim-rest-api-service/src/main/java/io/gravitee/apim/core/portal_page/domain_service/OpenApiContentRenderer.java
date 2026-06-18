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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiExposedEntrypointDomainService;
import io.gravitee.apim.core.api.domain_service.ApiPathExtractor;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.RenderedPageContent;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class OpenApiContentRenderer implements ContentRenderer {

    private final PortalNavigationEnclosingApiDomainService enclosingApiDomainService;
    private final ApiCrudService apiCrudService;
    private final ApiExposedEntrypointDomainService apiExposedEntrypointDomainService;
    private final OpenApiContentTransformer openApiContentTransformer;

    @Override
    public boolean appliesTo(PortalPageContent<?> content) {
        return content instanceof OpenApiPageContent;
    }

    @Override
    public RenderedPageContent render(PortalNavigationItem item, PortalPageContent<?> content) {
        var openApiPage = (OpenApiPageContent) content;
        var configuration = openApiPage.getViewerSettings();
        var rendered = RenderedPageContent.of(openApiPage.getContent().value(), PortalPageContentType.OPENAPI, configuration);

        if (!(configuration instanceof SwaggerUiConfiguration swaggerUiConfiguration)) {
            return rendered;
        }

        if (!swaggerUiConfiguration.entrypointsAsServers() && !swaggerUiConfiguration.contextPathAsServerPath()) {
            return rendered;
        }

        return RenderedPageContent.of(
            openApiContentTransformer.transform(
                rendered.value(),
                swaggerUiConfiguration,
                buildApiContext(content, (PortalNavigationPage) item)
            ),
            rendered.type(),
            rendered.configuration()
        );
    }

    private Optional<OpenApiContentTransformer.ApiContext> buildApiContext(PortalPageContent<?> content, PortalNavigationPage page) {
        return enclosingApiDomainService
            .findEnclosingApiId(content.getEnvironmentId(), page)
            .flatMap(apiCrudService::findById)
            .map(api ->
                new OpenApiContentTransformer.ApiContext(
                    apiExposedEntrypointDomainService
                        .get(content.getOrganizationId(), content.getEnvironmentId(), api)
                        .stream()
                        .map(entrypoint -> entrypoint.value())
                        .toList(),
                    ApiPathExtractor.extractPaths(api)
                        .stream()
                        .map(path -> path.getPath())
                        .findFirst()
                )
            );
    }
}
