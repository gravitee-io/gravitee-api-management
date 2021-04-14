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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.search.Indexable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(value = { "displayName" }, allowGetters = true)
public class UserEntity implements Indexable {

    /**
     * User identifier
     */
    private String id;

    /**
     * The user first name
     */
    private String firstname;

    /**
     * The user last name
     */
    private String lastname;

    /**
     * The user password
     */
    private String password;

    /**
     * The user email
     */
    private String email;

    /**
     * The user roles
     */
    private Set<UserRoleEntity> roles;

    /**
     * The user creation date
     */
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * The user last updated date
     */
    @JsonProperty("updated_at")
    private Date updatedAt;

    /**
     * The user picture
     */
    private String picture;

    /**
     * The source when user is coming from an external system (LDAP, ...)
     */
    private String source;

    /**
     * The user reference in the external source
     */
    private String sourceId;

    /**
     * The user last connection date
     */
    private Date lastConnectionAt;

    @JsonProperty("primary_owner")
    private boolean primaryOwner;

    private String status;

    private long loginCount;

    @JsonProperty("number_of_active_tokens")
    private int nbActiveTokens;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<UserRoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<UserRoleEntity> roles) {
        this.roles = roles;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Date getLastConnectionAt() {
        return lastConnectionAt;
    }

    public void setLastConnectionAt(Date lastConnectionAt) {
        this.lastConnectionAt = lastConnectionAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(long loginCount) {
        this.loginCount = loginCount;
    }

    public String getDisplayName() {
        String displayName;

        if ((firstname != null && !firstname.isEmpty()) || (lastname != null && !lastname.isEmpty())) {
            if (firstname != null && !firstname.isEmpty()) {
                displayName = firstname + ((lastname != null && !lastname.isEmpty()) ? ' ' + lastname : "");
            } else {
                displayName = lastname;
            }
        } else if (email != null && !email.isEmpty() && !"memory".equals(source)) {
            displayName = email;
        } else {
            displayName = sourceId;
        }

        return displayName;
    }

    public boolean isPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(boolean primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public int getNbActiveTokens() {
        return nbActiveTokens;
    }

    public void setNbActiveTokens(int nbActiveTokens) {
        this.nbActiveTokens = nbActiveTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "UserEntity{" +
            "id='" +
            id +
            '\'' +
            ", firstname='" +
            firstname +
            '\'' +
            ", lastname='" +
            lastname +
            '\'' +
            ", password='" +
            password +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", roles=" +
            roles +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            ", picture='" +
            picture +
            '\'' +
            ", source='" +
            source +
            '\'' +
            ", sourceId='" +
            sourceId +
            '\'' +
            ", lastConnectionAt=" +
            lastConnectionAt +
            ", primaryOwner=" +
            primaryOwner +
            ", status='" +
            status +
            '\'' +
            ", loginCount=" +
            loginCount +
            ", nbActiveTokens=" +
            nbActiveTokens +
            '}'
        );
    }
}
