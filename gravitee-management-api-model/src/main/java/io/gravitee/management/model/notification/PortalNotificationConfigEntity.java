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
package io.gravitee.management.model.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalNotificationConfigEntity {

    @JsonProperty("config_type")
    private NotificationConfigType configType;
    @JsonProperty("name")
    private String name = "Portal Notification";
    private String referenceType;
    private String referenceId;
    private String user;
    private List<String> hooks;


    public NotificationConfigType getConfigType() {
        return configType;
    }

    public void setConfigType(NotificationConfigType configType) {
        this.configType = configType;
    }

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

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalNotificationConfigEntity)) return false;
        PortalNotificationConfigEntity that = (PortalNotificationConfigEntity) o;
        return Objects.equals(referenceType, that.referenceType)
                && Objects.equals(referenceId, that.referenceId)
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }

    @Override
    public String toString() {
        return "PortalNotificationConfigEntity{" +
                "user='" + user + '\'' +
                ", configType='" + configType + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", hooks='" + hooks + '\'' +
                '}';
    }
}
