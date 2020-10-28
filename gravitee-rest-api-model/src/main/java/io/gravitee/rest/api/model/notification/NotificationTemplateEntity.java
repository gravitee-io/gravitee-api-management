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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class NotificationTemplateEntity {

    private String id;

    private String hook;

    private String scope;

    private String name;

    private String description;

    private String title;

    private String content;

    private NotificationTemplateType type;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    private Boolean enabled;

    @JsonIgnore
    private String templateName;

    public NotificationTemplateEntity() {
    }

    public NotificationTemplateEntity(String hook, String scope, String templateName, String name, String description, String title, String content, NotificationTemplateType type) {
        this.hook = hook;
        this.scope = scope;
        this.templateName = templateName;
        this.name = name;
        this.description = description;
        this.title = title;
        this.content = content;
        this.type = type;
        this.createdAt = new Date();
    }

    @JsonIgnore
    public String getTitleTemplateName() {
        return this.templateName + ".TITLE";
    }

    @JsonIgnore
    public String getContentTemplateName() {
        return this.templateName;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NotificationTemplateType getType() {
        return type;
    }

    public void setType(NotificationTemplateType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationTemplateEntity that = (NotificationTemplateEntity) o;
        return Objects.equals(hook, that.hook) &&
                Objects.equals(name, that.name) &&
                scope.equals(that.scope) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hook, name, scope, type);
    }
}
