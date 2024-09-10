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
package io.gravitee.apim.infra.domain_service.member;

import static org.mockito.Mockito.*;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CRDMembersDomainServiceImplTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("test-org").environmentId("test-env").build();

    @Mock
    MembershipService membershipService;

    @Mock
    RoleService roleService;

    CRDMembersDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new CRDMembersDomainServiceImpl(membershipService, roleService);
    }

    @Nested
    class Api {

        private static final String API_ID = "api-id";

        @BeforeEach
        void setUp() {
            when(roleService.findDefaultRoleByScopes(AUDIT_INFO.organizationId(), RoleScope.API))
                .thenReturn(List.of(RoleEntity.builder().id("default-api-role").build()));
        }

        @Test
        void should_set_members() {
            when(roleService.findByScopeAndName(RoleScope.API, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.API, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenReturn(RoleEntity.builder().id("role-2").build());

            cut.updateApiMembers(
                AUDIT_INFO,
                API_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-1"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-1",
                    "role-1"
                );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-2"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-2",
                    "role-2"
                );
        }

        @Test
        void should_use_default_role_when_member_with_no_role() {
            when(roleService.findByScopeAndName(RoleScope.API, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            cut.updateApiMembers(
                AUDIT_INFO,
                API_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role(null).id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-1"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-1",
                    "role-1"
                );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-2"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-2",
                    "default-api-role"
                );
        }

        @Test
        void should_use_default_role_when_member_with_unknown_role() {
            when(roleService.findByScopeAndName(RoleScope.API, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.API, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenThrow(new RoleNotFoundException("role-2"));

            cut.updateApiMembers(
                AUDIT_INFO,
                API_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-1"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-1",
                    "role-1"
                );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-2"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    "id-2",
                    "default-api-role"
                );
        }

        @Test
        void should_delete_orphans() {
            when(roleService.findByScopeAndName(RoleScope.API, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.API, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenThrow(new RoleNotFoundException("role-2"));

            when(
                membershipService.getMembersByReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID
                )
            )
                .thenReturn(Set.of(MemberEntity.builder().roles(List.of(RoleEntity.builder().id("role-3").build())).id("id-3").build()));

            when(roleService.findPrimaryOwnerRoleByOrganization(AUDIT_INFO.organizationId(), RoleScope.API))
                .thenReturn(RoleEntity.builder().id("po-role-id").build());

            cut.updateApiMembers(
                AUDIT_INFO,
                API_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    "api-id",
                    MembershipMemberType.USER,
                    "id-3"
                );
        }

        @Test
        void should_not_delete_po_role() {
            when(roleService.findByScopeAndName(RoleScope.API, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.API, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenThrow(new RoleNotFoundException("role-2"));

            when(
                membershipService.getMembersByReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    API_ID
                )
            )
                .thenReturn(
                    Set.of(MemberEntity.builder().roles(List.of(RoleEntity.builder().id("po-role-id").build())).id("id-3").build())
                );

            when(roleService.findPrimaryOwnerRoleByOrganization(AUDIT_INFO.organizationId(), RoleScope.API))
                .thenReturn(RoleEntity.builder().id("po-role-id").build());

            cut.updateApiMembers(
                AUDIT_INFO,
                API_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, never())
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.API,
                    "api-id",
                    MembershipMemberType.USER,
                    "id-3"
                );
        }
    }

    @Nested
    class Application {

        private static final String APPLICATION_ID = "app-id";

        @BeforeEach
        void setUp() {
            when(roleService.findDefaultRoleByScopes(AUDIT_INFO.organizationId(), RoleScope.APPLICATION))
                .thenReturn(List.of(RoleEntity.builder().id("default-app-role").build()));
        }

        @Test
        void should_set_members() {
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenReturn(RoleEntity.builder().id("role-2").build());

            cut.updateApplicationMembers(
                AUDIT_INFO,
                APPLICATION_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-1"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-1",
                    "role-1"
                );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-2"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-2",
                    "role-2"
                );
        }

        @Test
        void should_use_default_role_when_member_with_no_role() {
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            cut.updateApplicationMembers(
                AUDIT_INFO,
                APPLICATION_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role(null).id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-1"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-1",
                    "role-1"
                );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-2"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-2",
                    "default-app-role"
                );
        }

        @Test
        void should_use_default_role_when_member_with_unknown_role() {
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenThrow(new RoleNotFoundException("role-2"));

            cut.updateApplicationMembers(
                AUDIT_INFO,
                APPLICATION_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-1"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-1",
                    "role-1"
                );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-2"
                );

            verify(membershipService, times(1))
                .addRoleToMemberOnReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-2",
                    "default-app-role"
                );
        }

        @Test
        void should_delete_orphans() {
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenThrow(new RoleNotFoundException("role-2"));

            when(
                membershipService.getMembersByReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID
                )
            )
                .thenReturn(Set.of(MemberEntity.builder().roles(List.of(RoleEntity.builder().id("role-3").build())).id("id-3").build()));

            when(roleService.findPrimaryOwnerRoleByOrganization(AUDIT_INFO.organizationId(), RoleScope.APPLICATION))
                .thenReturn(RoleEntity.builder().id("po-role-id").build());

            cut.updateApplicationMembers(
                AUDIT_INFO,
                APPLICATION_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, times(1))
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-3"
                );
        }

        @Test
        void should_not_delete_po_role() {
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-1", AUDIT_INFO.organizationId()))
                .thenReturn(Optional.of(RoleEntity.builder().id("role-1").build()));

            when(roleService.findByScopeAndName(RoleScope.APPLICATION, "role-2", AUDIT_INFO.organizationId())).thenReturn(Optional.empty());

            when(roleService.findById("role-2")).thenThrow(new RoleNotFoundException("role-2"));

            when(
                membershipService.getMembersByReference(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID
                )
            )
                .thenReturn(
                    Set.of(MemberEntity.builder().roles(List.of(RoleEntity.builder().id("po-role-id").build())).id("id-3").build())
                );

            when(roleService.findPrimaryOwnerRoleByOrganization(AUDIT_INFO.organizationId(), RoleScope.APPLICATION))
                .thenReturn(RoleEntity.builder().id("po-role-id").build());

            cut.updateApplicationMembers(
                AUDIT_INFO,
                APPLICATION_ID,
                Set.of(
                    MemberCRD.builder().role("role-1").id("id-1").sourceId("source-id-1").source("test").build(),
                    MemberCRD.builder().role("role-2").id("id-2").sourceId("source-id-2").source("test").build()
                )
            );

            verify(membershipService, never())
                .deleteReferenceMember(
                    new ExecutionContext(AUDIT_INFO.organizationId(), AUDIT_INFO.environmentId()),
                    MembershipReferenceType.APPLICATION,
                    APPLICATION_ID,
                    MembershipMemberType.USER,
                    "id-3"
                );
        }
    }
}
