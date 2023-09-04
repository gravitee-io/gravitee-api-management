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

import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {

    public enum AuditEvent implements Audit.AuditEvent {
        USER_CREATED,
        USER_UPDATED,
        USER_CONNECTED,
        PASSWORD_RESET,
        PASSWORD_CHANGED,
        USER_CONFIRMED,
        USER_REJECTED,
    }

    /**
     * User identifier
     */
    private String id;

    /**
     * The organization id.
     */
    private String organizationId;

    /**
     * The source when user is coming from an external system (LDAP, ...)
     */
    private String source;

    /**
     * The user reference in the external source
     */
    private String sourceId;

    /**
     * The user password
     */
    private String password;

    /**
     * The user email
     */
    private String email;

    /**
     * The user first name
     */
    private String firstname;

    /**
     * The user last name
     */
    private String lastname;

    /**
     * The user creation date
     */
    private Date createdAt;

    /**
     * The user last updated date
     */
    private Date updatedAt;

    /**
     * The user last connection date
     */
    private Date lastConnectionAt;

    /**
     * The user first connection date
     */
    private Date firstConnectionAt;

    /**
     * The user picture
     */
    private String picture;

    private UserStatus status;

    /**
     * The user login count
     */
    private long loginCount;

    private Boolean newsletterSubscribed;

    public User(User cloned) {
        this.id = cloned.id;
        this.organizationId = cloned.organizationId;
        this.source = cloned.source;
        this.sourceId = cloned.sourceId;
        this.password = cloned.password;
        this.email = cloned.email;
        this.firstname = cloned.firstname;
        this.lastname = cloned.lastname;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.lastConnectionAt = cloned.lastConnectionAt;
        this.picture = cloned.picture;
        this.status = cloned.status;
        this.loginCount = cloned.loginCount;
        this.firstConnectionAt = cloned.firstConnectionAt;
        this.newsletterSubscribed = cloned.newsletterSubscribed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "User{" +
            "id='" +
            id +
            '\'' +
            ", organizationId='" +
            organizationId +
            '\'' +
            ", source='" +
            source +
            '\'' +
            ", sourceId='" +
            sourceId +
            '\'' +
            ", firstname='" +
            firstname +
            '\'' +
            ", lastname='" +
            lastname +
            '\'' +
            ", mail='" +
            email +
            '\'' +
            ", status='" +
            status +
            '\'' +
            ", loginCount='" +
            loginCount +
            '\'' +
            '}'
        );
    }
}
