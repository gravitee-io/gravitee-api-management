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

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Tag {

    public enum AuditEvent implements Audit.AuditEvent {
        TAG_CREATED,
        TAG_UPDATED,
        TAG_DELETED,
    }

    private String id;
    private String name;
    private String description;
    private List<String> restrictedGroups;
    private String referenceId;
    private TagReferenceType referenceType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return (
            Objects.equals(id, tag.id) &&
            Objects.equals(name, tag.name) &&
            Objects.equals(description, tag.description) &&
            Objects.equals(referenceId, tag.referenceId) &&
            Objects.equals(referenceType, tag.referenceType)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, restrictedGroups);
    }

    @Override
    public String toString() {
        return (
            "Tag{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", restrictedGroups=" +
            restrictedGroups +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            '}'
        );
    }
}
