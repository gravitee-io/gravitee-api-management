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
package io.gravitee.management.service;

import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.pagedresult.Metadata;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MembershipService {

    MemberEntity getMember(MembershipReferenceType referenceType, String referenceId, String userId, RoleScope roleScope);
    RoleEntity getRole(MembershipReferenceType referenceType, String referenceId, String userId, RoleScope roleScope);
    Set<RoleEntity> getRoles(MembershipReferenceType referenceType, Set<String> referenceIds, String userId, RoleScope roleScope);
    Set<MemberEntity> getMembers(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope);
    Set<MemberEntity> getMembers(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName);
    MemberEntity addOrUpdateMember(MembershipReference reference, MembershipUser user, MembershipRole role);
    void deleteMember(MembershipReferenceType referenceType, String referenceId, String userId);
    void deleteMembers(MembershipReferenceType referenceType, String referenceId);
    void transferApiOwnership(String apiId, MembershipUser user, RoleEntity newPrimaryOwnerRole);
    void transferApplicationOwnership(String applicationId, MembershipUser user, RoleEntity newPrimaryOwnerRole);
    Map<String, char[]> getMemberPermissions(ApiEntity api, String userId);
    Map<String, char[]> getMemberPermissions(ApplicationEntity application, String userId);
    Map<String, char[]> getMemberPermissions(GroupEntity group, String userId);
    boolean removeRole(MembershipReferenceType referenceType, String referenceId, String userId, RoleScope roleScope);
    void removeRoleUsage(RoleScope roleScope, String roleName, String newName);
    void removeUser(String userId);
    List<UserMembership> findUserMembership(String userId, MembershipReferenceType type);
    Metadata findUserMembershipMetadata(List<UserMembership> memberships, MembershipReferenceType type);
    int getNumberOfMembers(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope);

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
    }

    class MembershipUser {
        private final String id;
        private final String reference;

        public MembershipUser(String id, String reference) {
            this.id = id;
            this.reference = reference;
        }

        public String getId() {
            return id;
        }

        public String getReference() {
            return reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MembershipUser that = (MembershipUser) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            return reference != null ? reference.equals(that.reference) : that.reference == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            return result;
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
    }
}
