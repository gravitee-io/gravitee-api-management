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
package io.gravitee.apim.infra.crud_service.portal_page.adapter;

import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DefaultOpenApiProvider implements DefaultPortalPageContentProvider {

    private OpenApi defaultOpenApiContent;

    @Override
    public boolean appliesTo(PortalPageContentType contentType) {
        return contentType == PortalPageContentType.OPENAPI;
    }

    @Override
    public PortalPageContent<?> provide(String organizationId, String environmentId) {
        return new OpenApiPageContent(PortalPageContentId.random(), organizationId, environmentId, getDefaultOpenApiContent());
    }

    private OpenApi getDefaultOpenApiContent() {
        if (defaultOpenApiContent == null) {
            try {
                final var resource = new ClassPathResource("templates/default-portal-openapi-page-content.yaml");
                final var yaml = resource.getContentAsString(StandardCharsets.UTF_8);
                defaultOpenApiContent = OpenApi.of(yaml);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load default OpenAPI portal page template", e);
            }
        }
        return defaultOpenApiContent;
    }
}
