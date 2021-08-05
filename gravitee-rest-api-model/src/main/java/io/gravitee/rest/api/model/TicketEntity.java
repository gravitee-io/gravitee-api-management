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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketEntity {

    private String id;
    private String subject;
    private String content;
    private String application;
    private String api;

    @JsonProperty("from_user")
    private String fromUser;

    @JsonProperty("created_at")
    private Date createdAt;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicketEntity)) return false;
        TicketEntity that = (TicketEntity) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(subject, that.subject) &&
            Objects.equals(content, that.content) &&
            Objects.equals(application, that.application) &&
            Objects.equals(api, that.api) &&
            Objects.equals(fromUser, that.fromUser) &&
            Objects.equals(createdAt, that.createdAt)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subject, content, application, api, fromUser, createdAt);
    }

    @Override
    public String toString() {
        return (
            "TicketEntity{" +
            "id='" +
            id +
            '\'' +
            "subject='" +
            subject +
            '\'' +
            ", content='" +
            content +
            '\'' +
            ", application='" +
            application +
            '\'' +
            ", api='" +
            api +
            '\'' +
            ", fromUser='" +
            fromUser +
            '\'' +
            ", createdAt='" +
            createdAt +
            '\'' +
            '}'
        );
    }
}
