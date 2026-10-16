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
package io.gravitee.apim.core.portal_page.model;

import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class OpenApiPageContent extends PortalPageContent<OpenApi> {

    private static final PortalPageContentType TYPE = PortalPageContentType.OPENAPI;

    @Setter
    @Nonnull
    private OpenApi content;

    @Setter
    @Nonnull
    private OpenApiConfiguration viewerSettings;

    public OpenApiPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull OpenApi content
    ) {
        this(id, organizationId, environmentId, content, new RedocConfiguration(), null);
    }

    public OpenApiPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull OpenApi content,
        @Nonnull OpenApiConfiguration viewerSettings
    ) {
        this(id, organizationId, environmentId, content, viewerSettings, null);
    }

    public OpenApiPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull OpenApi content,
        @Nonnull OpenApiConfiguration viewerSettings,
        @Nullable AutomationMetadata automationMetadata
    ) {
        super(id, organizationId, environmentId, automationMetadata);
        this.content = content;
        this.viewerSettings = viewerSettings;
    }

    public static OpenApiPageContent create(@Nonnull String organizationId, @Nonnull String environmentId, @Nonnull String content) {
        return new OpenApiPageContent(PortalPageContentId.random(), organizationId, environmentId, OpenApi.of(content));
    }

    public PortalPageContentType getType() {
        return TYPE;
    }

    @Override
    public void update(@Nonnull UpdatePortalPageContent updatePortalPageContent) {
        var configuration = updatePortalPageContent.getConfiguration();
        if (configuration != null) {
            validateTryItUrl(configuration);
        }
        this.content = OpenApi.of(updatePortalPageContent.getContent());
        if (configuration != null) {
            this.viewerSettings = configuration;
        }
    }

    public void updateViewerSettings(@Nonnull OpenApiConfiguration viewerSettings) {
        validateTryItUrl(viewerSettings);
        this.viewerSettings = viewerSettings;
    }

    /** The portal makes the base URL the document's server, which Swagger UI calls to try the API: only http(s) can serve. */
    private static void validateTryItUrl(OpenApiConfiguration viewerSettings) {
        var tryItUrl = viewerSettings.tryItUrl();
        if (tryItUrl != null && !tryItUrl.isEmpty() && !isHttpUrl(tryItUrl)) {
            throw new InvalidUrlFormatException();
        }
    }

    private static boolean isHttpUrl(String url) {
        try {
            var uri = new URI(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getRawAuthority() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "OpenApi[id=" + getId() + ", content=" + content + "]";
    }
}
