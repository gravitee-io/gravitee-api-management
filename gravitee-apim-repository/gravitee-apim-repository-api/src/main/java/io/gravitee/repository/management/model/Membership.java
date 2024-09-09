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

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@AllArgsConstructor
public class Membership {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        MEMBERSHIP_CREATED,
        MEMBERSHIP_UPDATED,
        MEMBERSHIP_DELETED,
    }

    /**
     * Membership technical ID
     */
    private String id;

    /**
     * The memberId
     */
    private String memberId;

    /**
     * The member type
     */
    private MembershipMemberType memberType;

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
     * Role id
     */
    private String roleId;

    /**
     * The source of the membership (system, idp, ...)
     */
    private String source;

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

    public Membership(
        String id,
        String memberId,
        MembershipMemberType memberType,
        String referenceId,
        MembershipReferenceType referenceType,
        String roleId
    ) {
        this.id = id;
        this.memberId = memberId;
        this.memberType = memberType;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.roleId = roleId;
    }

    public Membership(Membership cloned) {
        this.id = cloned.id;
        this.memberId = cloned.memberId;
        this.memberType = cloned.memberType;
        this.referenceId = cloned.referenceId;
        this.referenceType = cloned.referenceType;
        this.roleId = cloned.roleId;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public MembershipMemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(MembershipMemberType memberType) {
        this.memberType = memberType;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Membership m = (Membership) o;
        return Objects.equals(id, m.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toString() {
        return (
            "Membership{" +
            "id='" +
            id +
            '\'' +
            ", memberId='" +
            memberId +
            '\'' +
            ", memberType='" +
            memberType +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            ", roleId='" +
            roleId +
            '\'' +
            ", source='" +
            source +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
