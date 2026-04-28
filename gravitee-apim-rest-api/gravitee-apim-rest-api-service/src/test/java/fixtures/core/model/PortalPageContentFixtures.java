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
package fixtures.core.model;

import io.gravitee.apim.core.async_api.AsyncApi;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiConfiguration;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;

public class PortalPageContentFixtures {

    public static final String ORGANIZATION_ID = "org-1234";
    public static final String ENVIRONMENT_ID = "env-1234";
    public static final String CONTENT_ID = "00000000-0000-0000-0000-000000000001";
    public static final String CONTENT = "# Welcome\n\nThis is a sample page content.";

    public static GraviteeMarkdownPageContent aGraviteeMarkdownPageContent() {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.of(CONTENT_ID),
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            new GraviteeMarkdown(CONTENT)
        );
    }

    public static GraviteeMarkdownPageContent aGraviteeMarkdownPageContent(String content) {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.of(CONTENT_ID),
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            new GraviteeMarkdown(content)
        );
    }

    public static GraviteeMarkdownPageContent aGraviteeMarkdownPageContent(
        PortalPageContentId id,
        String organizationId,
        String environmentId,
        String content
    ) {
        return new GraviteeMarkdownPageContent(id, organizationId, environmentId, new GraviteeMarkdown(content));
    }

    public static List<PortalPageContent<?>> samplePortalPageContents() {
        return List.of(aGraviteeMarkdownPageContent());
    }

    public static AsyncApiPageContent anAsyncApiPageContent() {
        return new AsyncApiPageContent(
            PortalPageContentId.of(CONTENT_ID),
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            new AsyncApi("asyncapi: '3.0.0'\ninfo:\n  title: Test")
        );
    }

    public static AsyncApiPageContent anAsyncApiPageContent(
        PortalPageContentId id,
        String organizationId,
        String environmentId,
        String content
    ) {
        return new AsyncApiPageContent(id, organizationId, environmentId, new AsyncApi(content));
    }

    public static OpenApiPageContent anOpenApiPageContent(
        PortalPageContentId id,
        String organizationId,
        String environmentId,
        String content,
        OpenApiConfiguration viewerSettings
    ) {
        return new OpenApiPageContent(id, organizationId, environmentId, OpenApi.of(content), viewerSettings);
    }
}
