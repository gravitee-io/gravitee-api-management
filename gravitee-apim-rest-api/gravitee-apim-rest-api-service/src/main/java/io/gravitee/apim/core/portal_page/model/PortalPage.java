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

import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

public final class PortalPage {

    @Nonnull
    private final PortalPageNavigationId id;

    @Nonnull
    private GraviteeMarkdown pageContent;

    @Setter
    @Getter
    private java.util.Date createdAt;

    @Setter
    @Getter
    private java.util.Date updatedAt;

    public PortalPage(@Nonnull PortalPageNavigationId id, @Nonnull GraviteeMarkdown pageContent) {
        this.id = id;
        this.pageContent = pageContent;
    }

    public static PortalPage create(GraviteeMarkdown pageContent) {
        return new PortalPage(PortalPageNavigationId.random(), pageContent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPage that = (PortalPage) o;
        return id.equals(that.id) && pageContent.equals(that.pageContent);
    }

    @Nonnull
    public PortalPageNavigationId getId() {
        return id;
    }

    @Nonnull
    public GraviteeMarkdown getPageContent() {
        return pageContent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pageContent);
    }

    @Override
    public String toString() {
        return "PortalPage[" + "id=" + id + ", " + "pageContent=" + pageContent + ']';
    }

    public void setContent(GraviteeMarkdown pageContent) {
        this.pageContent = pageContent;
    }
}
