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

import io.gravitee.repository.management.model.NotificationReferenceType;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalNotificationConfigPkMongo implements Serializable {

    private NotificationReferenceType referenceType;
    private String referenceId;
    private String user;

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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalNotificationConfigPkMongo)) return false;
        PortalNotificationConfigPkMongo portalNotificationConfigPkMongo = (PortalNotificationConfigPkMongo) o;
        return (
            Objects.equals(referenceType, portalNotificationConfigPkMongo.referenceType) &&
            Objects.equals(referenceId, portalNotificationConfigPkMongo.referenceId) &&
            Objects.equals(user, portalNotificationConfigPkMongo.user)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceType, referenceId, user);
    }

    @Override
    public String toString() {
        return (
            "PortalNotificationConfigPkMongo{" +
            "referenceType='" +
            referenceType +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", user='" +
            user +
            '\'' +
            '}'
        );
    }
}
