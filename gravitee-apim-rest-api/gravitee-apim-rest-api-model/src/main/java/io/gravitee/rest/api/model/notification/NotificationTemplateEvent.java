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

import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotificationTemplateEvent {

    private String organizationId;
    private NotificationTemplateEntity notificationTemplateEntity;

    public NotificationTemplateEvent(String organizationId, NotificationTemplateEntity notificationTemplateEntity) {
        this.organizationId = organizationId;
        this.notificationTemplateEntity = notificationTemplateEntity;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public NotificationTemplateEntity getNotificationTemplateEntity() {
        return notificationTemplateEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationTemplateEvent that = (NotificationTemplateEvent) o;
        return (
            Objects.equals(organizationId, that.organizationId) &&
            Objects.equals(notificationTemplateEntity, that.notificationTemplateEntity)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId, notificationTemplateEntity);
    }
}
