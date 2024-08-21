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

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerRemovalException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_EditMemberTest {

    private static final String ORG_ID = "DEFAULT";
    private static final String API_PO_ROLE_ID = "API_PRIMARY_OWNER_ROLE_ID";
    private static final String REFERENCE_ID = "REFERENCE_ID";
    private static final String MEMBER_ID = "MEMBER_ID";
    private static final String API_ID = "API_ID";

    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipRepository membershipRepository;

    @BeforeEach
    public void setUp() throws Exception {
        membershipService =
            new MembershipServiceImpl(
                null,
                null,
                null,
                null,
                null,
                null,
                membershipRepository,
                roleService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
    }

    @Test
    public void shouldNotRemoveApiPrimaryOwner() throws TechnicalException {
        when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), ORG_ID)).thenReturn(apiPrimaryOwnerRole());
        when(roleService.findByScopeAndName(RoleScope.INTEGRATION, PRIMARY_OWNER.name(), ORG_ID)).thenReturn(Optional.of(new RoleEntity()));

        Membership membership = new Membership();
        membership.setRoleId(API_PO_ROLE_ID);

        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any()))
            .thenReturn(Set.of(membership));

        assertThatThrownBy(() ->
                membershipService.updateRoleToMemberOnReference(
                    GraviteeContext.getExecutionContext(),
                    new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                    new MembershipService.MembershipMember(MEMBER_ID, REFERENCE_ID, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.API, "USER")
                )
            )
            .isInstanceOf(PrimaryOwnerRemovalException.class);
    }

    private static Optional<RoleEntity> apiPrimaryOwnerRole() {
        RoleEntity role = new RoleEntity();
        role.setName(PRIMARY_OWNER.name());
        role.setScope(RoleScope.API);
        role.setId(API_PO_ROLE_ID);
        role.setPermissions(Map.of());
        return Optional.of(role);
    }
}
