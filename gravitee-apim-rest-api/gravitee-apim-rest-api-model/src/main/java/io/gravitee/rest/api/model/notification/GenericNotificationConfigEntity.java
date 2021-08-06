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
package io.gravitee.rest.api.model.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GenericNotificationConfigEntity {

    @JsonProperty("config_type")
    private NotificationConfigType configType;

    private String id;
    private String name;
    private String referenceType;
    private String referenceId;
    private String notifier;
    private String config;
    private List<String> hooks;
    private boolean useSystemProxy;

    public NotificationConfigType getConfigType() {
        return configType;
    }

    public void setConfigType(NotificationConfigType configType) {
        this.configType = configType;
    }

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

    public String getNotifier() {
        return notifier;
    }

    public void setNotifier(String notifier) {
        this.notifier = notifier;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
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

    public boolean isUseSystemProxy() {
        return useSystemProxy;
    }

    public void setUseSystemProxy(boolean useSystemProxy) {
        this.useSystemProxy = useSystemProxy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericNotificationConfigEntity)) return false;
        GenericNotificationConfigEntity that = (GenericNotificationConfigEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notifier);
    }

    @Override
    public String toString() {
        return (
            "GenericNotificationConfigEntity{" +
            "id='" +
            id +
            '\'' +
            ", configType='" +
            configType +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", notifier='" +
            notifier +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", hooks='" +
            hooks +
            '\'' +
            ", useSystemProxy='" +
            useSystemProxy +
            '\'' +
            ", config='" +
            config +
            '\'' +
            '}'
        );
    }
}
