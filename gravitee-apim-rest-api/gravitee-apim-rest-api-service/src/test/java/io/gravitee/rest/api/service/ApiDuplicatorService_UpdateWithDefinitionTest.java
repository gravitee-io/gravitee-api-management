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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.ApiDuplicatorServiceImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDuplicatorService_UpdateWithDefinitionTest {

    private static final String API_ID = "id-api";
    private static final String SOURCE = "source";

    protected ApiDuplicatorService apiDuplicatorService;

    @Mock
    private ApiService apiService;

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

    @Before
    public void setup() {
        apiDuplicatorService =
            new ApiDuplicatorServiceImpl(
                httpClientService,
                importConfiguration,
                mediaService,
                objectMapper,
                apiMetadataService,
                membershipService,
                roleService,
                pageService,
                planService,
                groupService,
                userService,
                apiService
            );
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
        when(apiService.update(eq(API_ID), any(), anyBoolean())).thenReturn(apiEntity);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        when(roleService.findByScopeAndName(RoleScope.API, "API_PRIMARY_OWNER")).thenReturn(Optional.of(poRoleEntity));

        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "API_OWNER")).thenReturn(Optional.of(ownerRoleEntity));

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

        when(membershipService.getMembersByReference(MembershipReferenceType.API, API_ID)).thenReturn(Collections.singleton(po));

        UserEntity admin = new UserEntity();
        admin.setId(po.getId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(admin.getId())).thenReturn(admin);
        PageEntity existingPage = mock(PageEntity.class);

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));

        when(
            membershipService.addRoleToMemberOnReference(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenReturn(memberEntity);
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(pageService, times(1))
            .createOrUpdatePages(argThat(pagesList -> pagesList.size() == 2), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                "API_PRIMARY_OWNER"
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                "API_OWNER"
            );
        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    private ApiEntity prepareUpdateImportApiWithMembers(UserEntity admin, UserEntity user) {
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        apiEntity.setId(API_ID);
        apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        when(apiService.update(eq(API_ID), any(), anyBoolean())).thenReturn(apiEntity);

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);
        when(roleService.findByScopeAndName(RoleScope.API, "API_PRIMARY_OWNER")).thenReturn(Optional.of(poRole));

        RoleEntity ownerRole = new RoleEntity();
        ownerRole.setId("API_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "API_OWNER")).thenReturn(Optional.of(ownerRole));

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

        when(membershipService.getMembersByReference(MembershipReferenceType.API, API_ID))
            .thenReturn(new HashSet(Arrays.asList(owner, po)));

        admin.setId(po.getId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());

        user.setId(owner.getId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());

        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);

        return apiEntity;
    }

    @Test
    public void shouldUpdateImportApiWithMembers() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        po.setType(MembershipMemberType.USER);

        when(membershipService.getMembersByReference(MembershipReferenceType.API, API_ID)).thenReturn(new HashSet(Arrays.asList(po)));
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(pageService, never()).duplicatePages(anyList(), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                "API_PRIMARY_OWNER"
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                "API_OWNER"
            );
        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndUserAlreadyExists() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");

        when(userService.findById(admin.getId())).thenReturn(admin);
        when(userService.findById(user.getId())).thenReturn(user);

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(admin.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER")
            );
        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndAllMembersAlreadyExists() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");

        when(userService.findById(admin.getId())).thenReturn(admin);
        when(userService.findById(user.getId())).thenReturn(user);

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(admin.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER")
            );
        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithPages() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.update(eq(API_ID), any(), anyBoolean())).thenReturn(apiEntity);

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId("ref-admin");
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Collections.singletonList(poRole));

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            null,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(pageService, times(1))
            .createOrUpdatePages(argThat(pagesList -> pagesList.size() == 2), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                any(),
                any(),
                any()
            );
        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithOnlyDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.update(eq(API_ID), any(), anyBoolean())).thenReturn(apiEntity);

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            null,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class), eq(GraviteeContext.getCurrentEnvironment()));
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                any(),
                any(),
                any()
            );
        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    @Test
    public void shouldUpdateImportApiWithPlans() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);

        // plan1 is present both in existing api and in imported api definition
        // plan2 is present both in existing api and in imported api definition
        // plan3 is not present in existing api but added in imported api definition
        // plan4 is present in existing api, but not in imported api definition
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("plan-id1");
        PlanEntity plan2 = new PlanEntity();
        plan2.setId("plan-id2");
        PlanEntity plan3 = new PlanEntity();
        plan3.setId("plan-id3");
        PlanEntity plan4 = new PlanEntity();
        plan4.setId("plan-id4");
        when(planService.findByApi(apiEntity.getId())).thenReturn(Set.of(plan1, plan2, plan3, plan4));

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(planService, times(1)).findByApi(apiEntity.getId());
        // plan1, plan2 and plan4 has to be created or updated
        verify(planService, times(1)).createOrUpdatePlan(argThat(plan -> plan.getId().equals("plan-id1")), eq("DEFAULT"));
        verify(planService, times(1)).createOrUpdatePlan(argThat(plan -> plan.getId().equals("plan-id2")), eq("DEFAULT"));
        verify(planService, times(1)).createOrUpdatePlan(argThat(plan -> plan.getId().equals("plan-id4")), eq("DEFAULT"));
        // plan3 has to be deleted cause no more in imported file
        verify(planService, times(1)).delete("plan-id3");

        verify(planService, times(1)).anyPlanMismatchWithApi(eq(List.of("plan-id1", "plan-id2", "plan-id4")), eq(apiEntity.getId()));

        verifyNoMoreInteractions(planService);

        verify(apiService, times(1)).update(eq(API_ID), any(), anyBoolean());
        verify(apiService, never()).create(any(), any());
    }

    @Test
    public void shouldKeepExistingPlansDataIfNotInDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans-missingData.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);

        // plan1 had a description and a name before import
        // imported definition contains a new name, but doesn't specify any description
        PlanEntity plan1 = new PlanEntity();
        plan1.setId("plan-id1");
        plan1.setName("plan name before import");
        plan1.setDescription("plan description before import");
        when(planService.findByApi(apiEntity.getId())).thenReturn(Set.of(plan1));

        apiDuplicatorService.updateWithImportedDefinition(
            apiEntity.getId(),
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        // plan1 has been updated with new name from imported description. But kept his old description
        verify(planService, times(1))
            .createOrUpdatePlan(
                argThat(
                    plan ->
                        plan.getId().equals("plan-id1") &&
                        plan.getName().equals("new name from imported description") &&
                        plan.getDescription().equals("plan description before import")
                ),
                eq("DEFAULT")
            );
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldRaiseAnErrorWhenUpdatingAPlanBelongingToAnotherAPI() throws IOException {
        URL resource = Resources.getResource("io/gravitee/rest/api/management/service/import-api-update.definition+plans-missingData.json");
        String toBeImport = Resources.toString(resource, Charsets.UTF_8);

        when(planService.anyPlanMismatchWithApi(anyList(), anyString())).thenReturn(true);

        apiDuplicatorService.updateWithImportedDefinition(
            API_ID,
            toBeImport,
            "import",
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );
    }
}
