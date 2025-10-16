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
import java.util.Date;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

public abstract sealed class PageElement<T> permits PortalPage, PortalLink {

    @Nonnull
    private final PageId id;

    @Nonnull
    private T content;

    @Setter
    @Getter
    private Date createdAt;

    @Setter
    @Getter
    private Date updatedAt;

    protected PageElement(@Nonnull PageId id, @Nonnull T content) {
        this.id = id;
        this.content = content;
    }

    @Nonnull
    public PageId getId() {
        return id;
    }

    @Nonnull
    public T getContent() {
        return content;
    }

    public void setContent(@Nonnull T content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageElement<?> that = (PageElement<?>) o;
        return id.equals(that.id) && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content);
    }

    @Override
    public String toString() {
        return "PageElement[" + "id=" + id + ", " + "content=" + content + ']';
    }
}
