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

import io.gravitee.apim.core.async_api.AsyncApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class AsyncApiPageContent extends PortalPageContent<AsyncApi> {

    private static final PortalPageContentType TYPE = PortalPageContentType.ASYNCAPI;

    @Setter
    @Nonnull
    private AsyncApi content;

    public AsyncApiPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull AsyncApi content
    ) {
        this(id, organizationId, environmentId, content, null);
    }

    public AsyncApiPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull AsyncApi content,
        @Nullable AutomationMetadata automationMetadata
    ) {
        super(id, organizationId, environmentId, automationMetadata);
        this.content = content;
    }

    public static AsyncApiPageContent create(@Nonnull String organizationId, @Nonnull String environmentId, @Nonnull String content) {
        return new AsyncApiPageContent(PortalPageContentId.random(), organizationId, environmentId, AsyncApi.of(content));
    }

    public PortalPageContentType getType() {
        return TYPE;
    }

    @Override
    public void update(@Nonnull UpdatePortalPageContent updatePortalPageContent) {
        this.content = AsyncApi.of(updatePortalPageContent.getContent());
    }

    @Override
    public String toString() {
        return "AsyncApi[id=" + getId() + ", content=" + content + "]";
    }
}
