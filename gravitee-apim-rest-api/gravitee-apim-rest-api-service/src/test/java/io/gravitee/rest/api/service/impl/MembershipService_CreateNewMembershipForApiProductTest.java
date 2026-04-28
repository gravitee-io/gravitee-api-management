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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.MembershipAlreadyExistsException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipService_CreateNewMembershipForApiProductTest {

    private static final String API_PRODUCT_ID = "api-product-id-1";

    private MembershipServiceImpl membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private RoleService roleService;

    @Mock
    private IdentityService identityService;

    @Mock
    private Node node;

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @BeforeEach
    void setUp() {
        reset(membershipRepository, apiSearchService, userService, auditService, roleService, identityService);

        membershipService = new MembershipServiceImpl(
            identityService,
            userService,
            null,
            null,
            primaryOwnerService,
            null,
            membershipRepository,
            roleService,
            null,
            null,
            apiSearchService,
            null,
            null,
            null,
            auditService,
            null,
            null,
            node,
            objectMapper,
            commandRepository,
            null,
            null,
            null
        );

        mockRole();
    }

    @Test
    void should_create_membership_and_write_api_product_audit_log() throws Exception {
        String existingUserId = "existing-user-id";
        mockExistingUser(existingUserId);

        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API_PRODUCT);
        newMembership.setRoleId("API_PRODUCT_OWNER");
        newMembership.setReferenceId(API_PRODUCT_ID);
        newMembership.setMemberId(existingUserId);
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);

        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                existingUserId,
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.API_PRODUCT,
                API_PRODUCT_ID
            )
        ).thenReturn(Collections.emptySet(), Set.of(newMembership));

        MemberEntity createdMember = membershipService.createNewMembership(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.API_PRODUCT,
            API_PRODUCT_ID,
            existingUserId,
            null,
            "OWNER"
        );

        assertThat(createdMember).isNotNull();
        verify(auditService).createApiProductAuditLog(any(), any(), eq(API_PRODUCT_ID));
    }

    private void mockExistingUser(String existingUserId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(existingUserId);
        when(userService.findById(GraviteeContext.getExecutionContext(), existingUserId)).thenReturn(userEntity);
    }

    private void mockRole() {
        RoleEntity role = role("API_PRODUCT_OWNER");
        lenient()
            .when(roleService.findByScopeAndName(RoleScope.API_PRODUCT, "OWNER", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));
        when(roleService.findByIds(Set.of("API_PRODUCT_OWNER"))).thenReturn(Map.of("API_PRODUCT_OWNER", role));
    }

    private static RoleEntity role(String roleId) {
        Map<String, char[]> perms = new HashMap<>();
        for (Permission perm : OrganizationPermission.values()) {
            perms.put(perm.getName(), new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() });
        }
        RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.API_PRODUCT);
        role.setName("OWNER");
        role.setId(roleId);
        role.setPermissions(perms);
        return role;
    }

    private void mockExternalUser(String externalReference) {
        User externalUser = new User();
        externalUser.setSource("source");
        externalUser.setSourceId(externalReference);
        externalUser.setId("external-user-id");
        when(identityService.findByReference(externalReference)).thenReturn(Optional.of(externalUser));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("external-user-id");
        userEntity.setSource("source");
        userEntity.setSourceId(externalReference);
        when(userService.findBySource(anyString(), anyString(), anyString(), eq(false))).thenReturn(userEntity);

        mockExistingUser("external-user-id");
    }
}
