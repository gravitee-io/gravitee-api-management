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
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Application {

    public enum AuditEvent implements Audit.AuditEvent {
        APPLICATION_CREATED,
        APPLICATION_UPDATED,
        APPLICATION_ARCHIVED,
        APPLICATION_RESTORED,
    }

    /**
     * The application ID.
     */
    private String id;

    /**
     * The ID of the environment the application is attached to
     */
    private String environmentId;

    /**
     * The application name
     */
    private String name;

    /**
     * The application description
     */
    private String description;

    /**
     * The application picture
     */
    private String picture;

    /**
     * Domain used by the application, if relevant
     */
    private String domain;

    /**
     * The application creation date
     */
    private Date createdAt;

    /**
     * The application last updated date
     */
    private Date updatedAt;

    /**
     * the application group
     */
    private Set<String> groups;

    private ApplicationStatus status;

    private ApplicationType type;

    private Map<String, String> metadata;

    private boolean disableMembershipNotifications;

    private String background;

    private ApiKeyMode apiKeyMode = ApiKeyMode.UNSPECIFIED;

    public Application() {}

    public Application(Application cloned) {
        this.id = cloned.id;
        this.environmentId = cloned.environmentId;
        this.name = cloned.name;
        this.description = cloned.description;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.groups = cloned.groups;
        this.status = cloned.status;
        this.disableMembershipNotifications = cloned.disableMembershipNotifications;
        this.background = cloned.background;
        this.domain = cloned.domain;
        this.apiKeyMode = cloned.apiKeyMode;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public ApplicationType getType() {
        return type;
    }

    public void setType(ApplicationType type) {
        this.type = type;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public boolean isDisableMembershipNotifications() {
        return disableMembershipNotifications;
    }

    public void setDisableMembershipNotifications(boolean disableMembershipNotifications) {
        this.disableMembershipNotifications = disableMembershipNotifications;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public ApiKeyMode getApiKeyMode() {
        return apiKeyMode;
    }

    public void setApiKeyMode(ApiKeyMode apiKeyMode) {
        this.apiKeyMode = apiKeyMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "Application{" +
            "id='" +
            id +
            '\'' +
            ", environmentId='" +
            environmentId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", groups='" +
            groups +
            '\'' +
            ", status='" +
            status +
            '\'' +
            ", createdAt=" +
            createdAt +
            '\'' +
            ", updatedAt=" +
            updatedAt +
            '\'' +
            ", disableMembershipNotifications=" +
            disableMembershipNotifications +
            '}'
        );
    }
}
