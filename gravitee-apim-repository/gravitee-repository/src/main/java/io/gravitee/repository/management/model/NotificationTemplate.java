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

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotificationTemplate {

    public enum AuditEvent implements Audit.AuditEvent {
        NOTIFICATION_TEMPLATE_CREATED, NOTIFICATION_TEMPLATE_UPDATED
    }

    /**
     * Notification template ID
     */
    private String id;

    /**
     * Notification template linked hook
     */
    private String hook;

    /**
     * Notification template linked scope
     */
    private String scope;

    /**
     * The ID of the reference, the notification template is attached to
     */
    private String referenceId;

    /**
     * The type of the reference, the notification template is attached to
     */
    private NotificationTemplateReferenceType referenceType;

    /**
     * Notification template name
     */
    private String name;

    /**
     * Notification template description
     */
    private String description;

    /**
     * Notification template type. NOTIF | EMAIL
     */
    private NotificationTemplateType type;

    /**
     * Notification template title. Freemarker syntax.
     * It represents the title of a notif or the subject of an email.
     */
    private String title;

    /**
     * Notification template content. Freemarker syntax.
     */
    private String content;

    /**
     * Notification template creation date
     */
    private Date createdAt;

    /**
     * Notification template last updated date
     */
    private Date updatedAt;

    /**
     * Notification template status
     */
    private boolean enabled;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHook() {
        return hook;
    }

    public void setHook(String hook) {
        this.hook = hook;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public NotificationTemplateReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(NotificationTemplateReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NotificationTemplateType getType() {
        return type;
    }

    public void setType(NotificationTemplateType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationTemplate that = (NotificationTemplate) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
