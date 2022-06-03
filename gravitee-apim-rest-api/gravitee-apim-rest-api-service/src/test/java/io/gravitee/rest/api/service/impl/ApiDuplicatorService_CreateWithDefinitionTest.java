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
package io.gravitee.rest.api.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.impl.ApiDuplicatorServiceImpl;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class ApiDuplicatorService_CreateWithDefinitionTest {

    private static final String API_ID = "id-api";
    private static final String SOURCE = "source";

    @InjectMocks
    protected ApiDuplicatorServiceImpl apiDuplicatorService;

    @Spy
    private ObjectMapper objectMapper = (new ServiceConfiguration()).objectMapper();

    @Mock
    private ApiService apiService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private ImportConfiguration importConfiguration;

    @Mock
    private MediaService mediaService;

    @Before
    public void mockAuthenticatedUser() {
        final Authentication authentication = mock(Authentication.class);
        final UserDetails userDetails = new UserDetails("admin", "PASSWORD", Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
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
    public void shouldCreateImportApiWithMembersAndPages() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);
        when(userService.findBySource(anyString(), anyString(), eq(false))).thenReturn(new UserEntity());
        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        when(roleService.findByScopeAndName(RoleScope.API, "API_PRIMARY_OWNER")).thenReturn(Optional.of(poRoleEntity));

        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "API_OWNER")).thenReturn(Optional.of(ownerRoleEntity));

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        po.setType(MembershipMemberType.USER);
        MemberEntity owner = new MemberEntity();
        owner.setId("user");
        owner.setReferenceId(API_ID);
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Arrays.asList(ownerRoleEntity));
        owner.setType(MembershipMemberType.USER);
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));

        UserEntity admin = new UserEntity();
        admin.setId(po.getId());
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId(owner.getId());
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(memberEntity.getId())).thenReturn(admin);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1))
            .createOrUpdatePages(argThat(pagesList -> pagesList.size() == 2), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
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
    }

    @Test
    public void shouldCreateImportApiWithMembers() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(admin.getId())).thenReturn(admin);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);
        when(roleService.findByScopeAndName(RoleScope.API, "API_PRIMARY_OWNER")).thenReturn(Optional.of(poRoleEntity));

        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "API_OWNER")).thenReturn(Optional.of(ownerRoleEntity));

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        po.setType(MembershipMemberType.USER);
        MemberEntity owner = new MemberEntity();
        owner.setId("user");
        owner.setReferenceId(API_ID);
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setType(MembershipMemberType.USER);
        owner.setRoles(Arrays.asList(ownerRoleEntity));
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createAsideFolder(eq(API_ID), eq(GraviteeContext.getCurrentEnvironment()));
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
        verify(membershipService, never())
            .transferApiOwnership(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                any(),
                any(),
                any()
            );
    }

    @Test
    public void shouldCreateImportApiWithPages() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1))
            .createOrUpdatePages(argThat(pagesList -> pagesList.size() == 2), eq(GraviteeContext.getCurrentEnvironment()), eq(API_ID));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createAsideFolder(eq(API_ID), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionWithPrimaryOwner() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+primaryOwner.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createAsideFolder(eq(API_ID), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyNewDefinition() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-new-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createAsideFolder(eq(API_ID), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionEnumLowerCase() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition_enum_lowercase.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createAsideFolder(eq(API_ID), eq(GraviteeContext.getCurrentEnvironment()));
    }

    @Test
    public void shouldCreateImportApiWithPlans() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        String plan1newId = "310f7dd5-71b9-3bdd-b74d-4b76bc71c5b1";
        String plan2newId = "ed7bef56-b9ec-3c0a-9d21-02e046df429c";

        // check createWithApiDefinition has been called with newly generated plans IDs in API's definition
        verify(apiService, times(1))
            .createWithApiDefinition(
                any(),
                eq("admin"),
                argThat(
                    jsonNode -> {
                        JsonNode plansDefinition = jsonNode.path("plans");
                        return (
                            plansDefinition.get(0).get("id").asText().equals(plan1newId) &&
                            plansDefinition.get(1).get("id").asText().equals(plan2newId)
                        );
                    }
                )
            );

        // check find plans by API has been called once to remove potential pre-existing plans on target API
        verify(planService, times(1)).findByApi("id-api");
        // check planService has been called twice to create 2 plans, with same IDs as API definition
        verify(planService, times(1)).createOrUpdatePlan(argThat(plan -> plan.getId().equals(plan1newId)), any(String.class));
        verify(planService, times(1)).createOrUpdatePlan(argThat(plan -> plan.getId().equals(plan2newId)), any(String.class));
        // check that plan service verifies we are not updated a plan that does not belong to us
        verify(planService, times(1)).anyPlanMismatchWithApi(eq(List.of(plan1newId, plan2newId)), any(String.class));
        verifyNoMoreInteractions(planService);
    }

    @Test
    public void shouldCreateImportApiWithMetadata() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+metadata.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());

        verify(apiMetadataService, times(2)).update(any(UpdateApiMetadataEntity.class));
    }

    @Test
    public void shouldCreateImportApiEvenIfMemberRoleIsInvalid() throws IOException, TechnicalException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        when(apiService.createWithApiDefinition(any(), any(), any())).thenReturn(apiEntity);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(admin.getId())).thenReturn(admin);

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), eq(RoleScope.API))).thenReturn(poRoleEntity);

        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "API_OWNER")).thenReturn(Optional.of(ownerRoleEntity));

        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        po.setType(MembershipMemberType.USER);
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));

        when(
            membershipService.addRoleToMemberOnReference(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment(),
                MembershipReferenceType.API,
                API_ID,
                MembershipMemberType.USER,
                user.getId(),
                "API_OWNER"
            )
        )
            .thenThrow(new RoleNotFoundException("API_OWNER Not found"));

        apiDuplicatorService.createWithImportedDefinition(
            toBeImport,
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );

        verify(apiService, times(1)).createWithApiDefinition(any(), eq("admin"), any());
        verify(pageService, times(1)).createAsideFolder(eq(API_ID), eq(GraviteeContext.getCurrentEnvironment()));
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
        verify(membershipService, never()).transferApiOwnership(any(), any(), any(), any(), any());
    }
}
