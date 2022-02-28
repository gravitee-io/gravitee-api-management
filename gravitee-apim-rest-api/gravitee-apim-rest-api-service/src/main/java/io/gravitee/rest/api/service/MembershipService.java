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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MembershipService {
    MemberEntity addRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role
    );
    MemberEntity addRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String role
    );
    MemberEntity addRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String role,
        String source
    );
    void deleteMembership(final String organizationId, final String environmentId, String membershipId);
    void deleteReference(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId
    );
    void deleteReferenceMember(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId
    );
    void deleteReferenceMemberBySource(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String sourceId
    );
    List<UserMembership> findUserMembership(MembershipReferenceType referenceType, String userId);
    List<UserMembership> findUserMembershipBySource(MembershipReferenceType referenceType, String userId, String sourceId);
    Metadata findUserMembershipMetadata(List<UserMembership> memberships, MembershipReferenceType type);
    Page<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId, Pageable pageable);
    Set<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId);
    Page<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId, String role, Pageable pageable);
    Set<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId, String role);
    Page<MemberEntity> getMembersByReferenceAndRole(
        MembershipReferenceType referenceType,
        String referenceId,
        String role,
        Pageable pageable
    );
    Set<MemberEntity> getMembersByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, String role);
    Page<MemberEntity> getMembersByReferencesAndRole(
        MembershipReferenceType referenceType,
        List<String> referenceIds,
        String role,
        Pageable pageable
    );
    Set<MemberEntity> getMembersByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, String role);
    Set<MembershipEntity> getMembershipsByMember(MembershipMemberType memberType, String memberId);
    Set<MembershipEntity> getMembershipsByMemberAndReference(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType
    );
    Set<MembershipEntity> getMembershipsByMemberAndReferenceAndRole(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType,
        String role
    );
    Set<MembershipEntity> getMembershipsByMemberAndReferenceAndRoleIn(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    );
    Set<MembershipEntity> getMembershipsByMembersAndReference(
        MembershipMemberType memberType,
        List<String> membersId,
        MembershipReferenceType referenceType
    );
    Set<MembershipEntity> getMembershipsByReference(MembershipReferenceType referenceType, String referenceId);
    Set<MembershipEntity> getMembershipsByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, String role);
    Set<MembershipEntity> getMembershipsByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, String role);
    MembershipEntity getPrimaryOwner(final String organizationId, MembershipReferenceType referenceType, String referenceId);
    Set<RoleEntity> getRoles(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType, String memberId);
    MemberEntity getUserMember(final String environmentId, MembershipReferenceType referenceType, String referenceId, String userId);
    Map<String, char[]> getUserMemberPermissions(
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    );
    Map<String, char[]> getUserMemberPermissions(final String environmentId, ApiEntity api, String userId);
    Map<String, char[]> getUserMemberPermissions(final String environmentId, ApplicationEntity application, String userId);
    Map<String, char[]> getUserMemberPermissions(final String environmentId, GroupEntity group, String userId);
    Map<String, char[]> getUserMemberPermissions(final String environmentId, EnvironmentEntity environment, String userId);
    void removeRole(
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String roleId
    );
    void removeRoleUsage(String oldRoleId, String newRoleId);
    void removeMemberMemberships(MembershipMemberType memberType, String memberId);
    void transferApiOwnership(
        final String organizationId,
        final String environmentId,
        String apiId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    );

    void transferApplicationOwnership(
        final String organizationId,
        final String environmentId,
        String applicationId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    );
    MemberEntity updateRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role
    );
    List<MemberEntity> updateRolesToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        Collection<MembershipRole> roles,
        String source,
        boolean notify
    );
    List<MemberEntity> updateRolesToMemberOnReferenceBySource(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        Collection<MembershipRole> roles,
        String source
    );

    class MembershipReference {

        private final MembershipReferenceType type;
        private final String id;

        public MembershipReference(MembershipReferenceType type, String id) {
            this.type = type;
            this.id = id;
        }

        public MembershipReferenceType getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MembershipReference that = (MembershipReference) o;

            if (type != that.type) return false;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MembershipReference{" + "type=" + type + ", id='" + id + '\'' + '}';
        }
    }

    class MembershipMember {

        private final String memberId;
        private final String reference;
        private final MembershipMemberType memberType;

        public MembershipMember(String memberId, String reference, MembershipMemberType memberType) {
            this.memberId = memberId;
            this.reference = reference;
            this.memberType = memberType;
        }

        public String getMemberId() {
            return memberId;
        }

        public String getReference() {
            return reference;
        }

        public MembershipMemberType getMemberType() {
            return memberType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MembershipMember that = (MembershipMember) o;

            if (memberId != null ? !memberId.equals(that.memberId) : that.memberId != null) return false;
            return reference != null ? reference.equals(that.reference) : that.reference == null;
        }

        @Override
        public int hashCode() {
            int result = memberId != null ? memberId.hashCode() : 0;
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return (
                "MembershipMember{" +
                "memberId='" +
                memberId +
                '\'' +
                ", reference='" +
                reference +
                '\'' +
                ", memberType=" +
                memberType +
                '}'
            );
        }
    }

    class MembershipRole {

        private final RoleScope scope;
        private final String name;

        public MembershipRole(RoleScope scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        public RoleScope getScope() {
            return scope;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MembershipRole that = (MembershipRole) o;

            if (scope != that.scope) return false;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = scope.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MembershipRole{" + "scope=" + scope + ", name='" + name + '\'' + '}';
        }
    }

    class Membership {

        final MembershipReference reference;
        final MembershipMember member;
        final MembershipRole role;
        String source;

        public Membership(MembershipReference reference, MembershipMember member, MembershipRole role) {
            this.reference = reference;
            this.member = member;
            this.role = role;
        }

        public MembershipReference getReference() {
            return reference;
        }

        public MembershipMember getMember() {
            return member;
        }

        public MembershipRole getRole() {
            return role;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
