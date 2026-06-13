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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
        this.content = OpenApi.of(updatePortalPageContent.getContent());
        this.viewerSettings = updatePortalPageContent.getConfiguration() != null
            ? updatePortalPageContent.getConfiguration()
            : new RedocConfiguration();
    }

    public void updateViewerSettings(@Nonnull OpenApiConfiguration viewerSettings) {
        this.viewerSettings = viewerSettings;
    }

    @Override
    public String toString() {
        return "OpenApi[id=" + getId() + ", content=" + content + "]";
    }
}
