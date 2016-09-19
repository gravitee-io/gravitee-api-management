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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Membership {

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
     * Membership type
     */
    private String type;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
                Objects.equals(referenceType, m.referenceType) &&
                Objects.equals(type, m.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, referenceId, referenceType, type);
    }

    public String toString() {
        return "Membership{" +
                "userId='" + userId + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
