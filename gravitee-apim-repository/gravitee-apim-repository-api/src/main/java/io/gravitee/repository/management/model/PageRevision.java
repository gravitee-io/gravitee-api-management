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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;
import lombok.*;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PageRevision {

    private String pageId;
    private int revision;
    private String name;
    private String hash;
    private String content;
    private String contributor;
    private Date createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageRevision page = (PageRevision) o;
        return Objects.equals(pageId, page.pageId) && Objects.equals(revision, page.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, revision);
    }

    @Override
    public String toString() {
        return (
            "Page{" +
            "pageId='" +
            pageId +
            '\'' +
            ", revision=" +
            revision +
            ", name='" +
            name +
            '\'' +
            ", content='" +
            content +
            '\'' +
            ", hash='" +
            hash +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", contributor=" +
            contributor +
            '}'
        );
    }
}
