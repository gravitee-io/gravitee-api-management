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

import io.gravitee.repository.management.model.TagReferenceType;
import java.util.List;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "tags")
public class TagMongo {

    @Id
    private String id;

    private String name;
    private String description;
    private List<String> restrictedGroups;
    private String referenceId;
    private TagReferenceType referenceType;
    private List<String> flowIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRestrictedGroups() {
        return restrictedGroups;
    }

    public void setRestrictedGroups(List<String> restrictedGroups) {
        this.restrictedGroups = restrictedGroups;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public TagReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(TagReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public List<String> getFlowIds() {
        return flowIds;
    }

    public void setFlowIds(List<String> flowIds) {
        this.flowIds = flowIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagMongo)) return false;
        TagMongo tagMongo = (TagMongo) o;
        return Objects.equals(id, tagMongo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "TagMongo{" +
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
            ", flowsIds='" +
            flowIds +
            '\'' +
            '}'
        );
    }
}
