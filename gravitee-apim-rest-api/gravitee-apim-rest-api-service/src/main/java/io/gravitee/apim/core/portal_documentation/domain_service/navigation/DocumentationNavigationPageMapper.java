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
package io.gravitee.apim.core.portal_documentation.domain_service.navigation;

import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.Slug;
import jakarta.annotation.Nullable;

public final class DocumentationNavigationPageMapper {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;
    private static final int DEFAULT_ORDER = 0;

    private DocumentationNavigationPageMapper() {}

    public static PortalNavigationItem build(
        PortalNavigationItemId navigationItemId,
        PortalPageContentId contentId,
        @Nullable PortalNavigationItemContainer parent,
        String organizationId,
        String environmentId,
        AutomationMetadata meta,
        Slug segment
    ) {
        var create = CreatePortalNavigationItem.builder()
            .id(navigationItemId)
            .title(meta.name())
            .segment(segment.value())
            .area(AREA)
            .type(PortalNavigationItemType.PAGE)
            .order(meta.order().orElse(DEFAULT_ORDER))
            .portalPageContentId(contentId)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
        return PortalNavigationItem.from(create, organizationId, environmentId, parent);
    }

    public static void apply(
        PortalNavigationPage page,
        PortalPageContentId contentId,
        @Nullable PortalNavigationItemContainer parent,
        AutomationMetadata meta,
        Slug segment
    ) {
        page.setTitle(meta.name());
        page.setSegment(segment.value());
        page.setArea(AREA);
        page.setOrder(meta.order().orElse(DEFAULT_ORDER));
        page.setPortalPageContentId(contentId);
        page.setVisibility(PortalVisibility.PUBLIC);
        page.setPublished(true);
        if (parent == null) {
            page.markAsRoot();
        } else {
            page.updateParent(parent);
        }
    }

    public static PortalNavigationItemContainer phantomParent(@Nullable PortalNavigationItemId folderId) {
        if (folderId == null) return null;
        return new PortalNavigationItemContainer() {
            @Override
            public PortalNavigationItemId getId() {
                return folderId;
            }

            @Override
            public PortalNavigationItemId getRootId() {
                return folderId;
            }
        };
    }
}
