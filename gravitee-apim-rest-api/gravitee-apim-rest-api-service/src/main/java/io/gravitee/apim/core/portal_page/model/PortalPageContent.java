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

import jakarta.annotation.Nonnull;
import lombok.Getter;

@Getter
public abstract sealed class PortalPageContent permits GraviteeMarkdownPageContent {

    @Nonnull
    private final PortalPageContentId id;

    @Nonnull
    private final String organizationId;

    @Nonnull
    private final String environmentId;

    protected PortalPageContent(@Nonnull PortalPageContentId id, @Nonnull String organizationId, @Nonnull String environmentId) {
        this.id = id;
        this.organizationId = organizationId;
        this.environmentId = environmentId;
    }

    public abstract void update(@Nonnull UpdatePortalPageContent updatePortalPageContent);

    public abstract PortalPageContentType getType();

    public abstract String getContent();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPageContent that = (PortalPageContent) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
