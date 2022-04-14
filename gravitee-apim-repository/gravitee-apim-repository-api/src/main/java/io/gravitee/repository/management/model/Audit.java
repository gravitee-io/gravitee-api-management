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
import java.util.Map;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Audit {

    public enum AuditReferenceType {
        API,
        APPLICATION,
        ENVIRONMENT,
        ORGANIZATION,
    }

    public interface AuditEvent {
        String name();
    }

    public interface ApiAuditEvent extends AuditEvent {}

    public enum AuditProperties {
        PLAN,
        PAGE,
        API_KEY,
        METADATA,
        GROUP,
        USER,
        ROLE,
        API,
        APPLICATION,
        TAG,
        TENANT,
        CATEGORY,
        PARAMETER,
        DICTIONARY,
        API_HEADER,
        IDENTITY_PROVIDER,
        ENTRYPOINT,
        REQUEST_ID,
        CLIENT_REGISTRATION_PROVIDER,
        QUALITY_RULE,
        API_QUALITY_RULE,
        DASHBOARD,
        THEME,
        TOKEN,
        USER_FIELD,
        NOTIFICATION_TEMPLATE,
    }

    private String id;
    private String organizationId;
    private String environmentId;
    private String referenceId;
    private AuditReferenceType referenceType;
    private String user;
    private Date createdAt;
    private String event;
    private Map<String, String> properties;
    private String patch;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public AuditReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(AuditReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Audit audit = (Audit) o;
        return Objects.equals(id, audit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "Audit{" +
            "id='" +
            id +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            ", event='" +
            event +
            '\'' +
            ", properties='" +
            properties +
            '\'' +
            ", user='" +
            user +
            '\'' +
            ", createdAt='" +
            createdAt +
            '\'' +
            ", patch='" +
            patch +
            '\'' +
            '}'
        );
    }
}
