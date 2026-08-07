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
import jakarta.annotation.Nullable;
import lombok.Getter;

@Getter
public abstract sealed class PortalPageContent<T> permits GraviteeMarkdownPageContent, OpenApiPageContent, AsyncApiPageContent {

    @Nonnull
    private final PortalPageContentId id;

    @Nonnull
    private final String organizationId;

    @Nonnull
    private final String environmentId;

    /**
     * @deprecated superseded by {@link PortalNavigationItem#getAutomationMetadata()} — the nav
     * item is the thing every automation-managed resource type actually has (a Link has no
     * content object at all), so ownership metadata now lives there. Kept here, populated exactly
     * as before, until the read paths above are migrated off it; see the backfill upgrader
     * ({@code PortalNavigationItemAutomationMetadataUpgrader}) for the corresponding data copy.
     */
    @Deprecated
    @Nullable
    private AutomationMetadata automationMetadata;

    protected PortalPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nullable AutomationMetadata automationMetadata
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.environmentId = environmentId;
        this.automationMetadata = automationMetadata;
    }

    public abstract void update(@Nonnull UpdatePortalPageContent updatePortalPageContent);

    public final void update(@Nonnull UpdatePortalPageContent updatePortalPageContent, @Nullable AutomationMetadata automationMetadata) {
        this.automationMetadata = automationMetadata;
        update(updatePortalPageContent);
    }

    public abstract PortalPageContentType getType();

    public abstract T getContent();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPageContent<?> that = (PortalPageContent<?>) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
