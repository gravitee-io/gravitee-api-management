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
package io.gravitee.rest.api.model;

import java.util.Date;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@With
@Getter
@Setter
public class MembershipEntity {

    private String id;

    private String memberId;
    private MembershipMemberType memberType;

    private String referenceId;
    private MembershipReferenceType referenceType;

    private String roleId;

    private Date createdAt;

    private Date updatedAt;

    public MembershipEntity(MembershipEntity cloned) {
        this.id = cloned.id;
        this.memberId = cloned.memberId;
        this.memberType = cloned.memberType;
        this.referenceId = cloned.referenceId;
        this.referenceType = cloned.referenceType;
        this.roleId = cloned.roleId;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembershipEntity m = (MembershipEntity) o;
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
            ", role='" +
            roleId +
            '\'' +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            '}'
        );
    }
}
