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

import io.gravitee.apim.core.api.model.Api;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a portal navigation item that references an API.
 * This allows APIs to be added directly to the portal navigation menu.
 *
 * @author GraviteeSource Team
 */
@Getter
@SuperBuilder
public final class PortalNavigationApi extends PortalNavigationItem {

    private static final PortalNavigationItemType TYPE = PortalNavigationItemType.API;

    /**
     * The unique identifier of the referenced API.
     */
    @Setter
    @Nonnull
    private String apiId;

    /**
     * The referenced API definition.
     * This field is transient and populated by the UseCase, not persisted directly
     * in the navigation item repository.
     */
    @Setter
    private Api apiDefinition;

    public PortalNavigationApi(
        @Nonnull PortalNavigationItemId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull Integer order,
        @Nonnull String apiId,
        @Nonnull Boolean published,
        @Nonnull PortalVisibility visibility
    ) {
        super(id, organizationId, environmentId, title, area, order, published, visibility);
        this.apiId = apiId;
    }

    @Override
    public PortalNavigationItemType getType() {
        return TYPE;
    }

    @Override
    public void update(UpdatePortalNavigationItem navItem) {
        super.update(navItem);
        if (navItem.getApiId() != null) {
            this.setApiId(navItem.getApiId());
        }
    }
}
