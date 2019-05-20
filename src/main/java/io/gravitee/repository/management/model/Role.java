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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Role {
    public enum AuditEvent implements Audit.AuditEvent {
        ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED
    }
    private String name;
    private String referenceId;
    private RoleReferenceType referenceType;
    private RoleScope scope;
    private String description;
    private boolean defaultRole;
    private int[] permissions;
    private boolean system;
    private Date createdAt;
    private Date updatedAt;

    public Role(){}

    public Role(Role cloned) {
        this.name = cloned.name;
        this.referenceId = cloned.referenceId;
        this.referenceType = cloned.referenceType;
        this.scope = cloned.scope;
        this.description = cloned.description;
        this.defaultRole = cloned.defaultRole;
        this.permissions = cloned.permissions;
        this.system = cloned.system;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public RoleReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RoleReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public RoleScope getScope() {
        return scope;
    }
    public void setScope(RoleScope scope) {
        this.scope = scope;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultRole() {
        return defaultRole;
    }
    public void setDefaultRole(boolean defaultRole) {
        this.defaultRole = defaultRole;
    }

    public int[] getPermissions() {
        return permissions;
    }
    public void setPermissions(int[] permissions) {
        this.permissions = permissions;
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

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name) && Objects.equals(scope, role.scope) && Objects.equals(referenceId, role.referenceId) && referenceType == role.referenceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, name);
    }

    @Override
    public String toString() {
        return "Role{" +
                "name='" + name + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", scope='" + scope + '\'' +
                ", defaultRole='" + defaultRole + '\'' +
                ", system='" + system + '\'' +
                '}';
    }
}
