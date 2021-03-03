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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
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

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_CreateWithDefinitionTest {

    private static final String API_ID = "id-api";
    private static final String PLAN_ID = "my-plan";
    private static final String SOURCE = "source";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Spy
    private ObjectMapper objectMapper = (new ServiceConfiguration()).objectMapper();
    @Mock
    private Api api;
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
    private AuditService auditService;
    @Mock
    private IdentityService identityService;
    @Mock
    private SearchEngineService searchEngineService;
    @Mock
    private ParameterService parameterService;
    @Mock
    private VirtualHostService virtualHostService;
    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;
    @Mock
    private ApiMetadataService apiMetadataService;
    @Mock
    private AlertService alertService;
    @Spy
    private PolicyService policyService;
    @Mock
    private NotificationTemplateService notificationTemplateService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return null;
            }
            @Override
            public void setAuthentication(Authentication authentication) {
            }
        });
    }

    @Before
    public void setUp() {
        when(notificationTemplateService.resolveInlineTemplateWithParam(anyString(), any(Reader.class), any())).thenReturn("toDecode=decoded-value");
        when(parameterService.find(Key.API_PRIMARY_OWNER_MODE, ParameterReferenceType.ENVIRONMENT)).thenReturn("USER");
        reset(searchEngineService);
    }

    @Test
    public void shouldCreateImportApiWithMembersAndPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(userService.findBySource(anyString(), anyString(), eq(false))).thenReturn(new UserEntity());
        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findByScopeAndName(any(), eq("PRIMARY_OWNER"))).thenReturn(Optional.of(poRoleEntity));
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");


        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        MemberEntity owner = new MemberEntity();
        owner.setId("user");
        owner.setReferenceId(API_ID);
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Arrays.asList(ownerRoleEntity));
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
        when(userService.findById(admin.getId())).thenReturn(admin);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));
        when(membershipService.addRoleToMemberOnReference(any(), any(), any())).thenReturn(memberEntity);
        when(membershipService.addRoleToMemberOnReference(any(), any(), any())).thenReturn(memberEntity);
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(memberEntity.getId())).thenReturn(admin);
        when(pageService.createWithDefinition(any(), any())).thenReturn(new PageEntity());

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).createWithDefinition(eq(API_ID), anyString());
        verify(membershipService, times(1)).addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(admin.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, times(1)).addRoleToMemberOnReference(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, user.getId(), "API_OWNER");
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldCreateImportApiWithMembers() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
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
        when(groupService.findByEvent(any())).thenReturn(Collections.emptySet());

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setId("API_PRIMARY_OWNER");
        when(roleService.findByScopeAndName(any(), eq("PRIMARY_OWNER"))).thenReturn(Optional.of(poRoleEntity));
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setId("API_OWNER");


        MemberEntity po = new MemberEntity();
        po.setId("admin");
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Arrays.asList(poRoleEntity));
        MemberEntity owner = new MemberEntity();
        owner.setId("user");
        owner.setReferenceId(API_ID);
        owner.setReferenceType(MembershipReferenceType.API);
        owner.setRoles(Arrays.asList(ownerRoleEntity));
        when(membershipService.getMembersByReference(any(), any())).thenReturn(Collections.singleton(po));


        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRoles(Collections.singletonList(poRoleEntity));
        when(membershipService.addRoleToMemberOnReference(any(), any(), any())).thenReturn(memberEntity);

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(admin.getId(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, times(1)).addRoleToMemberOnReference(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, user.getId(), "API_OWNER");
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(membershipService, never()).transferApiOwnership(any(), any(), any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldCreateImportApiWithPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findById(admin.getId())).thenReturn(admin);
        when(pageService.createWithDefinition(any(), any())).thenReturn(new PageEntity());

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).createWithDefinition(eq(API_ID), anyString());
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinition() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
        verify(membershipService, times(1)).addRoleToMemberOnReference(
            new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
            new MembershipService.MembershipMember("admin", null, MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));

    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionWithPrimaryOwner() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+primaryOwner.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        when(userService.findById(any())).thenReturn(user);
        when(parameterService.find(Key.API_PRIMARY_OWNER_MODE, ParameterReferenceType.ENVIRONMENT)).thenReturn("HYBRID");

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));

        verify(membershipService, times(1)).addRoleToMemberOnReference(
            new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
            new MembershipService.MembershipMember("user", null, MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionWithPrimaryOwnerGroup() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+primaryOwnerGroup.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);

        GroupEntity group = new GroupEntity();
        group.setApiPrimaryOwner(user.getId());
        group.setId("group");

        when(groupService.findById(any())).thenReturn(group);
        when(parameterService.find(Key.API_PRIMARY_OWNER_MODE, ParameterReferenceType.ENVIRONMENT)).thenReturn("GROUP");

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));

        verify(membershipService, times(1)).addRoleToMemberOnReference(
            new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
            new MembershipService.MembershipMember("group", null, MembershipMemberType.GROUP),
            new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
    }

    @Test
    public void shouldCreateImportApiWithOnlyNewDefinition() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-new-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);
        when(userService.findById(admin.getId())).thenReturn(admin);

        ApiEntity apiEntityCreated = apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(1)).createPage(any(), any(NewPageEntity.class));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionEnumLowerCase() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition_enum_lowercase.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember("admin", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldCreateImportApiWithPlans() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        UserEntity user = new UserEntity();
        user.setId("user");
        user.setSource(SOURCE);
        user.setSourceId(API_ID);
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(planService, times(2)).create(any(NewPlanEntity.class));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldCreateImportApiWithMetadata() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+metadata.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        UserEntity admin = new UserEntity();
        admin.setId("admin");
        admin.setSource(SOURCE);
        admin.setSourceId(API_ID);
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createWithImportedDefinition(null, toBeImport, "admin");

        verify(apiMetadataService, times(1)).create(any(NewApiMetadataEntity.class));
        verify(apiMetadataService, times(2)).update(any(UpdateApiMetadataEntity.class));
        verify(membershipService, times(1)).addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember("admin", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }
}
