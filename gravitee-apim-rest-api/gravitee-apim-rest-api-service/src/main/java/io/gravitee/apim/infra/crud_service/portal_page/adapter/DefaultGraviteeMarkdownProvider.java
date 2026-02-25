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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DefaultGraviteeMarkdownProvider implements DefaultPortalPageContentProvider {

    private String defaultPortalPageContent;

    @Override
    public boolean appliesTo(PortalPageContentType contentType) {
        return contentType == PortalPageContentType.GRAVITEE_MARKDOWN;
    }

    @Override
    public PortalPageContent<?> provide(String organizationId, String environmentId) {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            organizationId,
            environmentId,
            new GraviteeMarkdown(getDefaultPortalPageContent())
        );
    }

    private String getDefaultPortalPageContent() {
        if (defaultPortalPageContent == null) {
            try {
                final var resource = new ClassPathResource("templates/default-portal-page-content.md");
                defaultPortalPageContent = resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load default portal page template", e);
            }
        }
        return defaultPortalPageContent;
    }
}
