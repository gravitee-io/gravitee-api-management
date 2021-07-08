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

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "page_revisions")
public class PageRevisionMongo extends Auditable {

    @Id
    private PageRevisionPkMongo id;

    private String name;
    private String content;
    private String hash;
    private String contributor;

    public PageRevisionPkMongo getId() {
        return id;
    }

    public void setId(PageRevisionPkMongo id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageRevisionMongo)) return false;
        PageRevisionMongo pageMongo = (PageRevisionMongo) o;
        return Objects.equals(id, pageMongo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "PageMongo{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", content='" +
            content +
            '\'' +
            ", hash='" +
            hash +
            '\'' +
            ", contributor='" +
            contributor +
            '\'' +
            "} " +
            super.toString()
        );
    }
}
