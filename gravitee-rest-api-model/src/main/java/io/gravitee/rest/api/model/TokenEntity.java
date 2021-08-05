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
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TokenEntity {

    private String id;
    private String name;
    private String token;

    @JsonProperty(value = "created_at")
    private Date createdAt;

    @JsonProperty(value = "expires_at")
    private Date expiresAt;

    @JsonProperty(value = "last_use_at")
    private Date lastUseAt;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getLastUseAt() {
        return lastUseAt;
    }

    public void setLastUseAt(Date lastUseAt) {
        this.lastUseAt = lastUseAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenEntity that = (TokenEntity) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(token, that.token) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(expiresAt, that.expiresAt) &&
            Objects.equals(lastUseAt, that.lastUseAt)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, token, createdAt, expiresAt, lastUseAt);
    }

    @Override
    public String toString() {
        return (
            "TokenEntity{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", token='" +
            token +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", expiresAt=" +
            expiresAt +
            ", lastUseAt=" +
            lastUseAt +
            '}'
        );
    }
}
