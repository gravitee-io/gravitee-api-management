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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class PortalNotificationConfig {
    private String user;
    private List<String> hooks;
    private NotificationReferenceType referenceType;
    private String referenceId;
    private Date createdAt;
    private Date updatedAt;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<String> getHooks() {
        return hooks;
    }

    public void setHooks(List<String> hooks) {
        this.hooks = hooks;
    }

    public NotificationReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(NotificationReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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
        if (o == null || getClass() != o.getClass()) return false;
        PortalNotificationConfig portalNotificationConfig = (PortalNotificationConfig) o;
        return Objects.equals(referenceType, portalNotificationConfig.referenceType)
                && Objects.equals(referenceId, portalNotificationConfig.referenceId)
                && Objects.equals(user, portalNotificationConfig.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceType, referenceId, user);
    }

    public String toString() {
        return "PortalNotificationConfig{" +
                "user='" + user + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", hooks='" + hooks + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
