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
package io.gravitee.apim.core.portal_page.service_provider;

import java.util.Optional;

public interface PortalNavigationTemplatingService {
    /**
     * Renders Gravitee Markdown documentation using the same FreeMarker model as legacy API pages:
     * {@code api} when {@code enclosingApiId} is present, otherwise environment {@code metadata} only.
     *
     * @throws io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException on invalid template or missing properties (navigation has no page "messages")
     */
    String renderGraviteeMarkdown(RenderPortalNavigationMarkdownInput input);

    record RenderPortalNavigationMarkdownInput(
        String rawMarkdown,
        String templateKey,
        String organizationId,
        String environmentId,
        Optional<String> enclosingApiId
    ) {}
}
