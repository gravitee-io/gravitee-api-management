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
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public final class PortalNavigationPage extends PortalNavigationItem {

    private static final PortalNavigationItemType TYPE = PortalNavigationItemType.PAGE;

    @Setter
    @Nonnull
    private PortalPageContentId portalPageContentId;

    PortalNavigationPage(
        @Nonnull PortalNavigationItemId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull Integer order,
        @Nonnull PortalPageContentId portalPageContentId,
        @Nonnull Boolean published,
        @Nonnull PortalVisibility visibility
    ) {
        super(id, organizationId, environmentId, title, area, order, published, visibility);
        this.portalPageContentId = portalPageContentId;
    }

    @Override
    public PortalNavigationItemType getType() {
        return TYPE;
    }
}
