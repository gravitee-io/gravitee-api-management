/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
public class BasicAuthCredentials implements Serializable {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        BASIC_AUTH_CREDENTIALS_CREATED,
        BASIC_AUTH_CREDENTIALS_RENEWED,
        BASIC_AUTH_CREDENTIALS_REVOKED,
    }

    private String id;
    private String username;
    private String password;

    @Builder.Default
    private List<String> subscriptions = new ArrayList<>();

    private String application;
    private String environmentId;
    private Date expireAt;
    private Date createdAt;
    private Date updatedAt;
    private boolean revoked;
    private Date revokedAt;

    public BasicAuthCredentials(BasicAuthCredentials cloned) {
        this.id = cloned.id;
        this.username = cloned.username;
        this.password = cloned.password;
        this.subscriptions = cloned.subscriptions;
        this.application = cloned.application;
        this.environmentId = cloned.environmentId;
        this.expireAt = cloned.expireAt;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.revoked = cloned.revoked;
        this.revokedAt = cloned.revokedAt;
    }

    public BasicAuthCredentials revoke() {
        var revoked = new BasicAuthCredentials(this);
        var now = new Date();
        revoked.setRevoked(true);
        revoked.setUpdatedAt(now);
        revoked.setRevokedAt(now);
        return revoked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicAuthCredentials that = (BasicAuthCredentials) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
