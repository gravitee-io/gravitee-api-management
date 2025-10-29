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
import lombok.Setter;

@Getter
public abstract class PortalNavigationItem {

    @Nonnull
    private final PortalPageNavigationId id;

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
    @Nullable
    private Integer order;

    protected PortalNavigationItem(
        @Nonnull PortalPageNavigationId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.environmentId = environmentId;
        this.title = title;
        this.area = area;
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
}
