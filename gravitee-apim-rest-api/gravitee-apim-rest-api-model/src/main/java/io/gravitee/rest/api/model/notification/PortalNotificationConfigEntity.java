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
package io.gravitee.rest.api.model.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Origin;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
public class PortalNotificationConfigEntity {

    @JsonProperty("config_type")
    private NotificationConfigType configType;

    @JsonProperty("name")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String name = "Console Notification";

    private String referenceType;
    private String referenceId;
    private String user;
    private List<String> hooks;
    private Set<String> groupHooks;
    private List<String> groups;
    private Origin origin;
    private String organizationId;

    public PortalNotificationConfigEntity() {
        origin = Origin.MANAGEMENT;
    }

    public boolean isDefaultEmpty() {
        return (
            (hooks == null || hooks.isEmpty()) &&
            (groups == null || groups.isEmpty()) &&
            (user != null && !user.isEmpty()) &&
            (referenceType != null && !referenceType.isEmpty()) &&
            (referenceId != null && !referenceId.isEmpty()) &&
            (organizationId != null && !organizationId.isEmpty())
        );
    }

    public static PortalNotificationConfigEntity newDefaultEmpty(
        String user,
        String referenceType,
        String referenceId,
        String organizationId
    ) {
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("User must not be empty");
        }
        if (referenceType == null || referenceType.isEmpty()) {
            throw new IllegalArgumentException("ReferenceType must not be empty");
        }
        if (referenceId == null || referenceId.isEmpty()) {
            throw new IllegalArgumentException("ReferenceId must not be empty");
        }
        if (organizationId == null || organizationId.isEmpty()) {
            throw new IllegalArgumentException("organizationId must not be empty");
        }
        PortalNotificationConfigEntity entity = new PortalNotificationConfigEntity();
        entity.setConfigType(NotificationConfigType.PORTAL);
        entity.setReferenceType(referenceType);
        entity.setReferenceId(referenceId);
        entity.setUser(user);
        entity.setHooks(Collections.emptyList());
        entity.setGroups(Collections.emptyList());
        entity.setOrganizationId(organizationId);
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalNotificationConfigEntity)) return false;
        PortalNotificationConfigEntity that = (PortalNotificationConfigEntity) o;
        return (
            Objects.equals(referenceType, that.referenceType) &&
            Objects.equals(referenceId, that.referenceId) &&
            Objects.equals(user, that.user)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }
}
