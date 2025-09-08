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

import javax.annotation.Nonnull;
import lombok.Getter;

public class PortalPage {

    @Nonnull
    @Getter
    private final PageId id;

    @Nonnull
    @Getter
    private final GraviteeMarkdown pageContent;

    PortalPage(@Nonnull PageId id, @Nonnull GraviteeMarkdown pageContent) {
        this.id = id;
        this.pageContent = pageContent;
    }

    public static PortalPage create(GraviteeMarkdown pageContent) {
        return new PortalPage(PageId.random(), pageContent);
    }

    public static PortalPage of(@Nonnull PageId id, @Nonnull GraviteeMarkdown pageContent) {
        return new PortalPage(id, pageContent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPage that = (PortalPage) o;
        return id.equals(that.id) && pageContent.equals(that.pageContent);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + pageContent.hashCode();
        return result;
    }
}
