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

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public final class PortalNavigationAgent extends PortalNavigationItem implements PortalNavigationItemContainer {

    private static final PortalNavigationItemType TYPE = PortalNavigationItemType.AGENT;
    private static final boolean TERMS_AND_CONDITIONS_ENABLED_BY_DEFAULT = true;

    @Setter
    @Nonnull
    private String agentId;

    @Setter
    @Nullable
    private PortalPageContentId termsAndConditionsPageContentId;

    @Setter
    @Builder.Default
    private boolean termsAndConditionsEnabled = TERMS_AND_CONDITIONS_ENABLED_BY_DEFAULT;

    @Builder.Default
    @Nonnull
    private List<PortalCategoryId> categoryIds = List.of();

    PortalNavigationAgent(
        @Nonnull PortalNavigationItemId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull Integer order,
        @Nonnull String agentId,
        @Nonnull Boolean published,
        @Nonnull PortalVisibility visibility,
        List<PortalCategoryId> categoryIds
    ) {
        super(id, organizationId, environmentId, title, area, order, published, visibility);
        this.agentId = agentId;
        this.categoryIds = normalizeCategoryIds(categoryIds);
        this.termsAndConditionsEnabled = TERMS_AND_CONDITIONS_ENABLED_BY_DEFAULT;
    }

    @Override
    public PortalNavigationItemType getType() {
        return TYPE;
    }

    @Override
    public void update(UpdatePortalNavigationItem navItem) {
        super.update(navItem);
        this.categoryIds = normalizeCategoryIds(navItem.getCategoryIds());
        if (navItem.getTermsAndConditionsEnabled() != null) {
            this.termsAndConditionsEnabled = navItem.getTermsAndConditionsEnabled();
        }
    }

    private static List<PortalCategoryId> normalizeCategoryIds(List<PortalCategoryId> categoryIds) {
        return Objects.requireNonNullElse(categoryIds, List.of());
    }
}
