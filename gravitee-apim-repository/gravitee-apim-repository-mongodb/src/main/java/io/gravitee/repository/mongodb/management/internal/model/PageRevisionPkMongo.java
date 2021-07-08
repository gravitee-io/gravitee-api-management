/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.internal.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRevisionPkMongo implements Serializable {

    private String pageId;
    private int revision;

    public PageRevisionPkMongo(String pageId, int revision) {
        this.pageId = pageId;
        this.revision = revision;
    }

    public PageRevisionPkMongo() {}

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageRevisionPkMongo)) return false;
        PageRevisionPkMongo that = (PageRevisionPkMongo) o;
        return Objects.equals(pageId, that.pageId) && Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, revision);
    }

    @Override
    public String toString() {
        return "PageRevisionPkMongo{" + "pageId='" + pageId + '\'' + ", revision=" + revision + '}';
    }
}
