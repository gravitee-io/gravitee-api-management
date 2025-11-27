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
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract sealed class PortalNavigationItem permits PortalNavigationPage, PortalNavigationLink, PortalNavigationFolder {

    @Nonnull
    private final PortalNavigationItemId id;

    @Setter
    @Nonnull
    private String organizationId;

    @Setter
    @Nonnull
    private String environmentId;

    @Setter
    @Nonnull
    private String title;

    @Setter
    @Nonnull
    private PortalArea area;

    @Setter
    @Nonnull
    private Integer order;

    @Setter
    @Nullable
    private PortalNavigationItemId parentId;

    protected PortalNavigationItem(
        @Nonnull PortalNavigationItemId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull Integer order
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.environmentId = environmentId;
        this.title = title;
        this.area = area;
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalNavigationItem that = (PortalNavigationItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "PortalNavigationItem[id=" + id + ", title=" + title + "]";
    }

    public static PortalNavigationItem from(CreatePortalNavigationItem item, String organizationId, String environmentId) {
        final var id = Optional.ofNullable(item.getId()).orElse(PortalNavigationItemId.random());
        final var title = item.getTitle();
        final var area = item.getArea();
        final var parentId = item.getParentId();
        final var contentId = item.getPortalPageContentId();
        final var url = item.getUrl();
        final var order = item.getOrder();

        final var newItem = switch (item.getType()) {
            case FOLDER -> new PortalNavigationFolder(id, organizationId, environmentId, title, area, order);
            case PAGE -> new PortalNavigationPage(id, organizationId, environmentId, title, area, order, contentId);
            case LINK -> new PortalNavigationLink(id, organizationId, environmentId, title, area, order, url);
        };
        newItem.setParentId(parentId);

        return newItem;
    }

    public void update(UpdatePortalNavigationItem navItem) {
        this.setTitle(navItem.getTitle().trim());
        this.setOrder(navItem.getOrder());
        this.setParentId(navItem.getParentId());
    }
}
