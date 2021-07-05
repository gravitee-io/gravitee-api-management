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
import java.util.List;
import java.util.Objects;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomUserField {
    public enum AuditEvent implements Audit.AuditEvent {
        CUSTOM_USER_FIELD_CREATED, CUSTOM_USER_FIELD_UPDATED, CUSTOM_USER_FIELD_DELETED
    }

    private String key;
    private String referenceId;
    private CustomUserFieldReferenceType referenceType;
    private String label;
    private MetadataFormat format;
    private boolean required;
    private List<String> values;
    private Date createdAt;
    private Date updatedAt;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public MetadataFormat getFormat() {
        return format;
    }

    public void setFormat(MetadataFormat format) {
        this.format = format;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
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


    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public CustomUserFieldReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(CustomUserFieldReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserField)) return false;
        CustomUserField that = (CustomUserField) o;
        return required == that.required &&
                Objects.equals(key, that.key) &&
                Objects.equals(referenceId, that.referenceId) &&
                referenceType == that.referenceType &&
                Objects.equals(label, that.label) &&
                format == that.format &&
                Objects.equals(values, that.values) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, referenceId, referenceType, label, format, required, values, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "CustomUserField{" +
                "key='" + key + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType=" + referenceType +
                ", label='" + label + '\'' +
                ", format=" + format +
                ", required=" + required +
                ", values=" + values +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
