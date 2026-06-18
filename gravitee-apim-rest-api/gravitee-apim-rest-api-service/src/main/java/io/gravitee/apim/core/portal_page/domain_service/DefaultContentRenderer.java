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
import io.gravitee.apim.core.portal_page.exception.RendererException;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.RenderedPageContent;

@DomainService
public class DefaultContentRenderer implements ContentRenderer {

    @Override
    public boolean appliesTo(PortalPageContent<?> content) {
        return content instanceof AsyncApiPageContent;
    }

    @Override
    public RenderedPageContent render(PortalNavigationItem item, PortalPageContent<?> content) {
        return switch (content) {
            case AsyncApiPageContent aapi -> RenderedPageContent.of(aapi.getContent().value(), PortalPageContentType.ASYNCAPI);
            default -> throw new RendererException("Content type not supported: " + content.getType());
        };
    }
}
