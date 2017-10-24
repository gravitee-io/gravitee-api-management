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

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Membership {
    public enum AuditEvent implements Audit.AuditEvent {
        MEMBERSHIP_CREATED, MEMBERSHIP_UPDATED, MEMBERSHIP_DELETED
    }
    /**
     * The userid
     */
    private String userId;

    /**
     * The external reference id. Depending on the reference type.
     * It could be the api id if the referenceType is API
     */
    private String referenceId;

    /**
     * the reference type. API or Application for example.
     * this helps to know the model of the referenceId
     */
    private MembershipReferenceType referenceType;

    /**
     * Roles
     */
    private Map<Integer, String> roles;

    /**
     * Creation date
     */
    private Date createdAt;

    /**
     * Last updated date
     */
    private Date updatedAt;

    public Membership() {
        super();
    }

    public Membership(String userId, String referenceId, MembershipReferenceType referenceType) {
        this.userId = userId;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public Membership(Membership cloned) {
        this.userId = cloned.userId;
        this.referenceId = cloned.referenceId;
        this.referenceType = cloned.referenceType;
        this.roles = new HashMap<>(cloned.roles);
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Map<Integer, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<Integer, String> roles) {
        this.roles = roles;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public MembershipReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(MembershipReferenceType referenceType) {
        this.referenceType = referenceType;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membership m = (Membership)o;
        return Objects.equals(userId, m.userId) &&
                Objects.equals(referenceId, m.referenceId) &&
                Objects.equals(referenceType, m.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, referenceId, referenceType);
    }

    public String toString() {
        return "Membership{" +
                "userId='" + userId + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
