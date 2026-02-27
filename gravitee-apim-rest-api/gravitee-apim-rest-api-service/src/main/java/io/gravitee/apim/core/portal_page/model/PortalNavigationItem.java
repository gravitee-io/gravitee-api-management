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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public abstract sealed class PortalNavigationItem
    permits PortalNavigationPage, PortalNavigationLink, PortalNavigationFolder, PortalNavigationApi {

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

    @Nullable
    private PortalNavigationItemId parentId;

    @Setter
    @Builder.Default
    @Nonnull
    private PortalNavigationItemId rootId = PortalNavigationItemId.zero();

    @Setter
    @Nonnull
    private Boolean published;

    @Setter
    @Nonnull
    private PortalVisibility visibility;

    protected PortalNavigationItem(
        @Nonnull PortalNavigationItemId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull Integer order,
        @Nonnull Boolean published,
        @Nonnull PortalVisibility visibility
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.environmentId = environmentId;
        this.title = title;
        this.area = area;
        this.order = order;
        this.published = published;
        this.visibility = visibility;
        this.rootId = id;
    }

    public abstract PortalNavigationItemType getType();

    public void markAsRoot() {
        this.parentId = null;
        this.rootId = this.id;
    }

    /**
     * Updates parentId and rootId atomically from the given parent container.
     */
    public void updateParent(@Nonnull PortalNavigationItemContainer parent) {
        this.parentId = parent.getId();
        this.rootId = parent.getRootId();
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

    public static PortalNavigationItem from(
        CreatePortalNavigationItem item,
        String organizationId,
        String environmentId,
        @Nullable PortalNavigationItemContainer parent
    ) {
        final var id = Optional.ofNullable(item.getId()).orElse(PortalNavigationItemId.random());
        final var title = item.getTitle();
        final var area = item.getArea();
        final var contentId = item.getPortalPageContentId();
        final var url = item.getUrl();
        final var apiId = item.getApiId();
        final var order = item.getOrder();
        final var visibility = null != item.getVisibility() ? item.getVisibility() : PortalVisibility.PUBLIC;
        final var published = null != item.getPublished() ? item.getPublished() : false;

        final PortalNavigationItem newItem = switch (item.getType()) {
            case FOLDER -> new PortalNavigationFolder(id, organizationId, environmentId, title, area, order, published, visibility);
            case PAGE -> new PortalNavigationPage(id, organizationId, environmentId, title, area, order, contentId, published, visibility);
            case LINK -> new PortalNavigationLink(id, organizationId, environmentId, title, area, order, url, published, visibility);
            case API -> new PortalNavigationApi(id, organizationId, environmentId, title, area, order, apiId, published, visibility);
        };
        if (parent == null) {
            newItem.markAsRoot();
        } else {
            newItem.updateParent(parent);
        }
        return newItem;
    }

    public void update(UpdatePortalNavigationItem navItem) {
        this.setTitle(navItem.getTitle().trim());
        this.setOrder(navItem.getOrder());
        this.setPublished(navItem.getPublished());
        this.setVisibility(navItem.getVisibility());
    }
}
