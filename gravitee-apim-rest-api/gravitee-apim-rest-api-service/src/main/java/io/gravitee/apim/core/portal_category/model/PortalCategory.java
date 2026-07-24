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
package io.gravitee.apim.core.portal_category.model;

import jakarta.annotation.Nonnull;
import lombok.Getter;

/**
 * @author GraviteeSource Team
 */
@Getter
public class PortalCategory {

    @Nonnull
    private final PortalCategoryId id;

    @Nonnull
    private final String environmentId;

    private String title;
    private String description;
    private boolean visible;

    /** Raw constructor - only used for database mapping, use {@link #create} or {@link #of} instead. */
    private PortalCategory(PortalCategoryId id, String environmentId, String title, String description, boolean visible) {
        this.id = id;
        this.environmentId = environmentId;
        this.title = title;
        this.description = description;
        this.visible = visible;
    }

    public static PortalCategory create(String environmentId, String title, String description, boolean visible) {
        return new PortalCategory(PortalCategoryId.random(), environmentId, title, description, visible);
    }

    /** Reconstitute a portal category from a known id (database mapping). */
    public static PortalCategory of(PortalCategoryId id, String environmentId, String title, String description, boolean visible) {
        return new PortalCategory(id, environmentId, title, description, visible);
    }

    public void update(UpdatePortalCategory update) {
        this.title = update.getTitle();
        this.description = update.getDescription();
        this.visible = update.isVisible();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalCategory that = (PortalCategory) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
