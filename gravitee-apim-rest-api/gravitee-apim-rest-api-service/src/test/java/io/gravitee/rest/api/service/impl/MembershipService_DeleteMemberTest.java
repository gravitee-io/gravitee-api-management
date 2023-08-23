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
<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/rest/api/service/impl/MembershipService_DeleteMemberTest.java
import io.gravitee.rest.api.service.exceptions.ApiPrimaryOwnerRemovalException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
=======
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerRemovalException;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
>>>>>>> a0f8d71591 (fix: do not allow to delete application primay owner via management API):gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/rest/api/service/MembershipService_DeleteMemberTest.java
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_DeleteMemberTest {

    private static final String ORG_ID = "DEFAULT";
    private static final String ENV_ID = "DEFAULT";
    private static final String API_PO_ROLE_ID = "123";
    private static final String APPLICATION_PO_ROLE_ID = "222";
    private static final String REFERENCE_ID = "456";
    private static final String API_ID = "789";

    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipRepository membershipRepository;

<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/rest/api/service/impl/MembershipService_DeleteMemberTest.java
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
                null
            );
    }

    @Test
    public void shouldNotRemoveApiPrimaryOwner() throws TechnicalException {
        when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), ORG_ID)).thenReturn(apiPrimaryOwnerRole());
=======
    @Before
    public void setup() throws TechnicalException {
        when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), ORG_ID))
            .thenReturn(primaryOwnerRole(RoleScope.API, API_PO_ROLE_ID));
        when(roleService.findByScopeAndName(RoleScope.APPLICATION, PRIMARY_OWNER.name(), ORG_ID))
            .thenReturn(primaryOwnerRole(RoleScope.APPLICATION, APPLICATION_PO_ROLE_ID));
    }
>>>>>>> a0f8d71591 (fix: do not allow to delete application primay owner via management API):gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/rest/api/service/MembershipService_DeleteMemberTest.java

    @Test(expected = PrimaryOwnerRemovalException.class)
    public void shouldNotRemoveApiPrimaryOwner() throws TechnicalException {
        Membership membership = new Membership();
        membership.setRoleId(API_PO_ROLE_ID);

        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any()))
            .thenReturn(Set.of(membership));

        assertThatThrownBy(() ->
                membershipService.deleteReferenceMember(
                    GraviteeContext.getExecutionContext(),
                    MembershipReferenceType.API,
                    API_ID,
                    MembershipMemberType.USER,
                    REFERENCE_ID
                )
            )
            .isInstanceOf(ApiPrimaryOwnerRemovalException.class);
    }

    @Test
    public void shouldNotDeleteApiPrimaryOwner() throws TechnicalException {
        when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), ORG_ID)).thenReturn(apiPrimaryOwnerRole());

        Membership membership = new Membership();
        membership.setRoleId(API_PO_ROLE_ID);

        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any()))
            .thenReturn(Set.of(membership));

        assertThatThrownBy(() -> membershipService.deleteMemberForApi(GraviteeContext.getExecutionContext(), API_ID, REFERENCE_ID))
            .isInstanceOf(ApiPrimaryOwnerRemovalException.class);
    }

    @Test(expected = PrimaryOwnerRemovalException.class)
    public void shouldNotRemoveApplicationPrimaryOwner() throws TechnicalException {
        Membership membership = new Membership();
        membership.setRoleId(APPLICATION_PO_ROLE_ID);

        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any()))
            .thenReturn(Set.of(membership));

        membershipService.deleteReferenceMember(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.APPLICATION,
            API_ID,
            MembershipMemberType.USER,
            REFERENCE_ID
        );
    }

    private static Optional<RoleEntity> primaryOwnerRole(RoleScope scope, String roleId) {
        RoleEntity role = new RoleEntity();
        role.setName(PRIMARY_OWNER.name());
        role.setScope(scope);
        role.setId(roleId);
        return Optional.of(role);
    }
}
