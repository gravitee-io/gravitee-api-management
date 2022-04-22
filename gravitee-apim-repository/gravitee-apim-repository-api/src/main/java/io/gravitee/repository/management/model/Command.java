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

public class Command {

    private String id;
    private String environmentId;
    private String organizationId;
    private String from;
    private String to;
    private List<String> tags;
    private String content;
    private List<String> acknowledgments;
    private Date expiredAt;
    private Date createdAt;
    private Date updatedAt;

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getAcknowledgments() {
        return acknowledgments;
    }

    public void setAcknowledgments(List<String> acknowledgments) {
        this.acknowledgments = acknowledgments;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Date expiredAt) {
        this.expiredAt = expiredAt;
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
        Command msg = (Command) o;
        return Objects.equals(id, msg.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "Command{" +
            "id='" +
            id +
            '\'' +
            ", environmentId='" +
            environmentId +
            '\'' +
            ", organizationId='" +
            organizationId +
            '\'' +
            ", from='" +
            from +
            '\'' +
            ", to='" +
            to +
            '\'' +
            ", tags=" +
            tags +
            ", content='" +
            content +
            '\'' +
            ", acknowledgments=" +
            acknowledgments +
            ", expiredAt=" +
            expiredAt +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
