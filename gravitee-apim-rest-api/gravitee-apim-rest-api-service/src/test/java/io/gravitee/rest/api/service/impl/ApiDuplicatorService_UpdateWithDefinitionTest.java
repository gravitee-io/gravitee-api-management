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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiImportException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDuplicatorService_UpdateWithDefinitionTest {

    private static final String API_ID = "id-api";
    private static final String SOURCE = "source";
    private static final String PO_ROLE_ID = "API_PRIMARY_OWNER";
    private static final String OWNER_ROLE_ID = "API_OWNER";

    @InjectMocks
    protected ApiDuplicatorServiceImpl apiDuplicatorService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PlanConverter planConverter;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private GroupService groupService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private MediaService mediaService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ApiIdsCalculatorService apiIdsCalculatorService;

    @Before
    public void mockUserWithPermissions() {
        Authentication authentication = mock(Authentication.class);
        UserDetails authenticatedUserDetails = new UserDetails("admin", "PASSWORD", Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(authenticatedUserDetails);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        GraviteeContext.cleanContext();
    }

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndPages() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any())).thenReturn(apiEntity);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId(PO_ROLE_ID);
        poRoleEntity.setScope(RoleScope.API);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);

        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId(OWNER_ROLE_ID);
        ownerRoleEntity.setScope(RoleScope.API);

        when(roleService.findByIdAndOrganizationId(anyString(), eq(GraviteeContext.getExecutionContext().getOrganizationId())))
            .thenAnswer(invocationOnMock -> {
                if (PO_ROLE_ID.equals(invocationOnMock.getArgument(0))) {
                    return Optional.of(poRoleEntity);
                } else if (OWNER_ROLE_ID.equals(invocationOnMock.getArgument(0))) {
                    return Optional.of(ownerRoleEntity);
                }
                return Optional.empty();
            });

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId("ref-admin");
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Collections.singletonList(poRoleEntity));
        po.setType(MembershipMemberType.USER);

        MemberEntity owner = new MemberEntity();
        owner.setId("owner");
        owner.setReferenceId("ref-user");
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Collections.singletonList(ownerRoleEntity));
        owner.setType(MembershipMemberType.USER);

        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID))
            .thenReturn(Collections.singleton(po));

        UserEntity admin = new UserEntity();
        admin.setId(po.getId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(GraviteeContext.getExecutionContext(), admin.getId())).thenReturn(admin);
        PageEntity existingPage = mock(PageEntity.class);

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));

        when(membershipService.addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any(), any(), any()))
            .thenReturn(memberEntity);
        when(userService.findBySource(GraviteeContext.getCurrentOrganization(), user.getSource(), user.getSourceId(), false))
            .thenReturn(user);

        mockApiIdRecalculation();
        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(pageService, times(1))
            .createOrUpdatePages(eq(GraviteeContext.getExecutionContext()), argThat(pagesList -> pagesList.size() == 2), eq(API_ID));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                PO_ROLE_ID
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                OWNER_ROLE_ID
            );
        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    private ApiEntity prepareUpdateImportApiWithMembers(String apiId, UserEntity admin, UserEntity user) {
        return prepareUpdateImportApiWithMembersAndGroups(apiId, admin, user, null);
    }

    private ApiEntity prepareUpdateImportApiWithMembersAndGroups(String apiId, UserEntity admin, UserEntity user, String groupId) {
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(apiId);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        apiEntity.setId(apiId);
        apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        if (groupId != null) {
            apiEntity.setGroups(Set.of(groupId));
        }
        when(apiService.update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any())).thenReturn(apiEntity);

        RoleEntity poRole = new RoleEntity();
        poRole.setId(PO_ROLE_ID);
        poRole.setScope(RoleScope.API);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);

        RoleEntity ownerRole = new RoleEntity();
        ownerRole.setId(OWNER_ROLE_ID);
        ownerRole.setScope(RoleScope.API);

        when(roleService.findByIdAndOrganizationId(anyString(), eq(GraviteeContext.getExecutionContext().getOrganizationId())))
            .thenAnswer(invocationOnMock -> {
                if (PO_ROLE_ID.equals(invocationOnMock.getArgument(0))) {
                    return Optional.of(poRole);
                } else if (OWNER_ROLE_ID.equals(invocationOnMock.getArgument(0))) {
                    return Optional.of(ownerRole);
                }
                return Optional.empty();
            });

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId("ref-admin");
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Collections.singletonList(poRole));
        po.setType(MembershipMemberType.USER);

        MemberEntity owner = new MemberEntity();
        owner.setId("owner");
        owner.setReferenceId("ref-user");
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Collections.singletonList(ownerRole));
        owner.setType(MembershipMemberType.USER);

        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID))
            .thenReturn(new HashSet(Arrays.asList(owner, po)));

        admin.setId(po.getId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());

        user.setId(owner.getId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());

        when(userService.findBySource(GraviteeContext.getCurrentOrganization(), user.getSource(), user.getSourceId(), false))
            .thenReturn(user);

        return apiEntity;
    }

    @Test
    public void shouldUpdateImportApiWithMembers() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(API_ID, admin, user);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId(PO_ROLE_ID);
        poRoleEntity.setScope(RoleScope.API);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        po.setType(MembershipMemberType.USER);

        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID))
            .thenReturn(new HashSet(Arrays.asList(po)));
        when(userService.findById(GraviteeContext.getExecutionContext(), admin.getId())).thenReturn(admin);

        mockApiIdRecalculation();
        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(pageService, never()).createOrUpdatePages(eq(GraviteeContext.getExecutionContext()), anyList(), eq(API_ID));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                PO_ROLE_ID
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                OWNER_ROLE_ID
            );
        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndUserAlreadyExists() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(API_ID, admin, user);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId(PO_ROLE_ID);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId(OWNER_ROLE_ID);

        when(userService.findById(GraviteeContext.getExecutionContext(), admin.getId())).thenReturn(admin);
        when(userService.findById(GraviteeContext.getExecutionContext(), user.getId())).thenReturn(user);

        mockApiIdRecalculation();

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(pageService, never()).createPage(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(admin.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER")
            );
        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndAllMembersAlreadyExists() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(API_ID, admin, user);
        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId(PO_ROLE_ID);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId(OWNER_ROLE_ID);

        when(userService.findById(GraviteeContext.getExecutionContext(), admin.getId())).thenReturn(admin);
        when(userService.findById(GraviteeContext.getExecutionContext(), user.getId())).thenReturn(user);

        mockApiIdRecalculation();
        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(pageService, never()).createPage(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(admin.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER")
            );
        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithPages() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any())).thenReturn(apiEntity);

        RoleEntity poRole = new RoleEntity();
        poRole.setId(PO_ROLE_ID);

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId("ref-admin");
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Collections.singletonList(poRole));

        mockApiIdRecalculation();

        GroupEntity knownGroup = new GroupEntity();
        knownGroup.setId("known_group_id");
        knownGroup.setName("known group name");
        when(groupService.findByName(GraviteeContext.getExecutionContext().getEnvironmentId(), "known group name"))
            .thenReturn(List.of(knownGroup));
        when(groupService.findByName(GraviteeContext.getExecutionContext().getEnvironmentId(), "group_id")).thenReturn(List.of());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(pageService, times(1))
            .createOrUpdatePages(
                eq(GraviteeContext.getExecutionContext()),
                argThat(pagesList -> {
                    boolean pageSizeOk = pagesList.size() == 4;
                    boolean accessControlOk = true;
                    for (PageEntity pageEntity : pagesList) {
                        if (pageEntity.getOrder() == 3) {
                            accessControlOk =
                                accessControlOk &&
                                pageEntity.getAccessControls() != null &&
                                pageEntity.getAccessControls().contains(new AccessControlEntity("known_group_id", "GROUP"));
                        } else if (pageEntity.getOrder() == 4) {
                            // unknown group
                            accessControlOk =
                                accessControlOk && (pageEntity.getAccessControls() == null || pageEntity.getAccessControls().isEmpty());
                        }
                    }

                    return pageSizeOk && accessControlOk;
                }),
                eq(API_ID)
            );
        verify(membershipService, never()).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
        verify(groupService, times(1)).findByName(GraviteeContext.getExecutionContext().getEnvironmentId(), "known group name");
        verify(groupService, times(1)).findByName(GraviteeContext.getExecutionContext().getEnvironmentId(), "group_id");
    }

    private void mockApiIdRecalculation() {
        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any()))
            .thenAnswer(invocationOnMock -> {
                // In this case, the ApiIdsCalculator service would have completed the api id
                final ImportApiJsonNode apiJsonNode = invocationOnMock.getArgument(1, ImportApiJsonNode.class);
                apiJsonNode.setId(invocationOnMock.getArgument(2, String.class));
                return apiJsonNode;
            });
    }

    @Test
    public void shouldUpdateImportApiWithOnlyDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any())).thenReturn(apiEntity);

        RoleEntity poRole = new RoleEntity();
        poRole.setId(PO_ROLE_ID);

        mockApiIdRecalculation();

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(pageService, never()).createPage(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithPlans() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembersAndGroups(API_ID, admin, user, "my_group_id");
        apiEntity.setId("id-api");
        apiEntity.setCrossId("api-cross-id");
        // plan1 is present both in existing api and in imported api definition
        // plan2 is present both in existing api and in imported api definition
        // plan3 is not present in existing api but added in imported api definition
        // plan4 is present in existing api, but not in imported api definition
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("plan-id1");
        plan1.setCrossId("plan-cross-id-1");
        PlanEntity plan2 = new PlanEntity();
        plan2.setId("plan-id2");
        plan2.setCrossId("plan-cross-id-2");
        PlanEntity plan3 = new PlanEntity();
        plan3.setId("plan-id3");
        PlanEntity plan4 = new PlanEntity();
        plan4.setId("plan-id4");

        when(planService.findByApi(GraviteeContext.getExecutionContext(), apiEntity.getId()))
            .thenReturn(Set.of(plan1, plan2, plan3, plan4));

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        GroupEntity knownGroup = new GroupEntity();
        knownGroup.setId("my_group_id");
        knownGroup.setName("MY Group");
        when(groupService.findByName(GraviteeContext.getExecutionContext().getEnvironmentId(), "My Group")).thenReturn(List.of(knownGroup));

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(groupService, times(2)).findByName(GraviteeContext.getExecutionContext().getEnvironmentId(), "My Group");

        verify(planService, times(2)).findByApi(GraviteeContext.getExecutionContext(), apiEntity.getId());
        // plan1, plan2 and plan4 has to be created or updated
        verify(planService, times(1))
            .createOrUpdatePlan(eq(GraviteeContext.getExecutionContext()), argThat(plan -> plan.getId().equals("plan-id1")));
        verify(planService, times(1))
            .createOrUpdatePlan(
                eq(GraviteeContext.getExecutionContext()),
                argThat(plan -> plan.getId().equals("plan-id2") && plan.getExcludedGroups().contains(knownGroup.getId()))
            );
        verify(planService, times(1))
            .createOrUpdatePlan(eq(GraviteeContext.getExecutionContext()), argThat(plan -> plan.getId().equals("plan-id4")));
        // plan3 has to be deleted cause no more in imported file
        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), "plan-id3");

        verify(planService, times(1)).anyPlanMismatchWithApi(eq(List.of("plan-id1", "plan-id2", "plan-id4")), eq(apiEntity.getId()));

        verifyNoMoreInteractions(planService);

        verify(apiService, times(1)).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any());
        verify(apiService, never()).create(eq(GraviteeContext.getExecutionContext()), any(), any());
    }

    @Test
    public void shouldKeepExistingPlansDataIfNotInDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans-missingData.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(API_ID, admin, user);
        apiEntity.setCrossId("api-cross-id");

        // plan1 had a description and a name before import
        // imported definition contains a new name, but doesn't specify any description
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("plan-id1");
        plan1.setCrossId("plan-cross-id-1");
        plan1.setName("plan name before import");
        plan1.setDescription("plan description before import");
        when(planService.findByApi(GraviteeContext.getExecutionContext(), apiEntity.getId())).thenReturn(Set.of(plan1));

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        // plan1 has been updated with new name from imported description. But kept his old description
        verify(planService, times(1))
            .createOrUpdatePlan(
                eq(GraviteeContext.getExecutionContext()),
                argThat(plan ->
                    plan.getId().equals("plan-id1") &&
                    plan.getName().equals("new name from imported description") &&
                    plan.getDescription().equals("plan description before import")
                )
            );
    }

    @Test(expected = ApiImportException.class)
    public void shouldRaiseAnErrorWhenUpdatingAPlanBelongingToAnotherAPI() throws IOException {
        URL resource = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans-missingData.json");
        String toBeImport = Resources.toString(resource, Charsets.UTF_8);

        ApiEntity api = new ApiEntity();
        api.setId("id-api");
        api.setCrossId("api-cross-id");

        PlanEntity apiPlan = new PlanEntity();
        apiPlan.setId("plan-id1");
        apiPlan.setCrossId("plan-cross-id-1");

        when(planService.anyPlanMismatchWithApi(anyList(), eq("id-api"))).thenReturn(true);

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), API_ID, toBeImport);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldRaiseAForbiddenAccessException_whenUserNotAuthenticated() throws IOException {
        // unauthenticate user
        SecurityContextHolder.setContext(new SecurityContextImpl());

        URL resource = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans-missingData.json");
        String toBeImport = Resources.toString(resource, Charsets.UTF_8);

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), API_ID, toBeImport);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldRaiseAForbiddenAccessException_whenUserHasNoRequiredPermission() throws IOException {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(API_DEFINITION), eq("id-api"), eq(UPDATE)))
            .thenReturn(false);

        URL resource = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans-missingData.json");
        String toBeImport = Resources.toString(resource, Charsets.UTF_8);

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), API_ID, toBeImport);
    }

    @Test
    public void shouldUpdateWithKubernetesOrigin() throws IOException {
        // For api coming from kubernetes operator, ids are managed by the operator itself and must remain the same to keep consistency.
        String apiId = "a409499e-e447-38fd-a3f0-a7f17bd67226";
        String apiCrossId = "ffffffff-ffff-ffff-ffff-ffffffffffff";
        String planId1 = "3f78a156-952e-3d98-8b04-bb6ec0f5bc72";
        String planCrossId1 = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String planId2 = "3fde2343-dbb5-385b-8ff7-9fe121b810b9";
        String planCrossId2 = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-update-kubernetes-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(apiId, admin, user);
        apiEntity.setId(apiId);
        apiEntity.setCrossId(apiCrossId);
        PlanEntity plan1 = new PlanEntity();
        plan1.setId(planId1);
        plan1.setCrossId(planCrossId1);
        plan1.setApi(apiId);
        PlanEntity plan2 = new PlanEntity();
        plan2.setId(planId2);
        plan2.setCrossId(planCrossId2);
        plan2.setApi(apiId);

        when(planService.findByApi(GraviteeContext.getExecutionContext(), apiEntity.getId())).thenReturn(Set.of(plan1, plan2));
        when(apiService.update(eq(GraviteeContext.getExecutionContext()), eq(apiId), any())).thenReturn(apiEntity);

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);

        verify(planService, times(2)).findByApi(GraviteeContext.getExecutionContext(), apiEntity.getId());
    }

    @Test(expected = ApiDefinitionVersionNotSupportedException.class)
    public void shouldNotUpdateIfDefinitionV1() throws TechnicalException, IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.v1.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);

        when(apiIdsCalculatorService.recalculateApiDefinitionIds(any(), any(), any())).then(AdditionalAnswers.returnsSecondArg());

        apiDuplicatorService.updateWithImportedDefinition(GraviteeContext.getExecutionContext(), apiEntity.getId(), toBeImport);
    }
}
