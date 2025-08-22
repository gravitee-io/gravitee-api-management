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

import java.util.UUID;
import javax.annotation.Nonnull;

public class PageId {

    @Nonnull
    private final UUID id;

    private PageId(UUID id) {
        this.id = id;
    }

    public static PageId random() {
        return new PageId(UUID.randomUUID());
    }

    public static PageId of(String id) {
        return new PageId(UUID.fromString(id));
    }

    public UUID id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageId pageId = (PageId) o;
        return id.equals(pageId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
