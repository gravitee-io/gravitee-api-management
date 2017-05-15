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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Metadata {

    private String key;
    private String referenceId;
    private MetadataReferenceType referenceType;
    private String name;
    private MetadataFormat format;
    private String value;
    private Date createdAt;
    private Date updatedAt;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public MetadataReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(MetadataReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetadataFormat getFormat() {
        return format;
    }

    public void setFormat(MetadataFormat format) {
        this.format = format;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Metadata)) return false;
        Metadata metadata = (Metadata) o;
        return Objects.equals(key, metadata.key) &&
                Objects.equals(referenceId, metadata.referenceId) &&
                referenceType == metadata.referenceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, referenceId, referenceType);
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "key='" + key + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType=" + referenceType +
                ", name='" + name + '\'' +
                ", format=" + format +
                ", value='" + value + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
