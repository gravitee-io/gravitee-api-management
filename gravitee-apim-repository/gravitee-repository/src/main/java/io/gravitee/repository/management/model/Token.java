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
public class Token {
    public enum AuditEvent implements Audit.AuditEvent {
        TOKEN_CREATED, TOKEN_DELETED
    }

    private String id;
    private String token;
    private String referenceType;
    private String referenceId;
    private String name;
    private Date expiresAt;
    private Date createdAt;
    private Date lastUseAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
        Token token1 = (Token) o;
        return Objects.equals(id, token1.id) &&
                Objects.equals(token, token1.token) &&
                Objects.equals(referenceType, token1.referenceType) &&
                Objects.equals(referenceId, token1.referenceId) &&
                Objects.equals(name, token1.name) &&
                Objects.equals(expiresAt, token1.expiresAt) &&
                Objects.equals(createdAt, token1.createdAt) &&
                Objects.equals(lastUseAt, token1.lastUseAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token, referenceType, referenceId, name, expiresAt, createdAt, lastUseAt);
    }

    @Override
    public String toString() {
        return "Token{" +
                "id='" + id + '\'' +
                ", token='" + token + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", name='" + name + '\'' +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", lastUseAt=" + lastUseAt +
                '}';
    }
}
