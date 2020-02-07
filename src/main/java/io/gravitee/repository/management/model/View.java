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
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class View {
    public enum AuditEvent implements Audit.AuditEvent {
        VIEW_CREATED, VIEW_UPDATED, VIEW_DELETED
    }

    public static final String ALL_ID = "all";

    private String id;
    private String environmentId;
    private String name;
    private String description;
    private boolean defaultView;
    private boolean hidden;
    private int order;
    private String highlightApi;
    private String picture;
    private Date createdAt;
    private Date updatedAt;

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

    public boolean isDefaultView() {
        return defaultView;
    }

    public void setDefaultView(boolean defaultView) {
        this.defaultView = defaultView;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getHighlightApi() {
        return highlightApi;
    }

    public void setHighlightApi(String highlightApi) {
        this.highlightApi = highlightApi;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
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
        if (!(o instanceof View)) return false;
        View view = (View) o;
        return Objects.equals(id, view.id) &&
                Objects.equals(name, view.name) &&
                Objects.equals(description, view.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "View{" +
                "id='" + id + '\'' +
                ", environmentId='" + environmentId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", defaultView='" + defaultView + '\'' +
                ", hidden='" + hidden + '\'' +
                ", order='" + order + '\'' +
                ", highlightApi='" + highlightApi + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
