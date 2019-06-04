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
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;

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
public class ApiService_UpdateWithDefinitionTest {

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
    private ObjectMapper objectMapper = new GraviteeMapper();
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
    private AuditService auditService;
    @Mock
    private SearchEngineService searchEngineService;
    @Mock
    private ParameterService parameterService;

    @Before
    public void init() {
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        po.setReferenceId("ref-admin");
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setReferenceId("ref-user");
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(admin.getId())).thenReturn(admin);
        PageEntity existingPage = mock(PageEntity.class);
        when(pageService.search(any())).thenReturn(Collections.singletonList(existingPage), Collections.emptyList());

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(Collections.singleton(memberEntity));
        when(membershipService.addOrUpdateMember(any(), any(), any())).thenReturn(memberEntity);
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(pageService, times(1)).update(any(), any(UpdatePageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(admin.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(user.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    private ApiEntity prepareUpdateImportApiWithMembers(UserEntity admin, UserEntity user) throws TechnicalException {
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        apiEntity.setId(API_ID);
        apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        po.setReferenceId("ref-admin");
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        owner.setReferenceId("ref-user");
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());

        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());

        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);

        return apiEntity;
    }

    @Test
    public void shouldUpdateImportApiWithMembers() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);

        MemberEntity poMember = new MemberEntity();
        poMember.setId("admin");
        poMember.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(new HashSet(Arrays.asList(poMember)));
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(admin.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(user.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndUserAlreadyExists() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        MemberEntity userMember = new MemberEntity();
        userMember.setId("user");
        userMember.setRole("OWNER");
        MemberEntity poMember = new MemberEntity();
        poMember.setId("admin");
        poMember.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(new HashSet(Arrays.asList(userMember, poMember)));
        when(userService.findById(admin.getId())).thenReturn(admin);
        when(userService.findById(user.getId())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(admin.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(user.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndAllMembersAlreadyExists() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        MemberEntity userMember = new MemberEntity();
        userMember.setId("user");
        userMember.setRole("OWNER");
        MemberEntity adminMember = new MemberEntity();
        adminMember.setId("admin");
        adminMember.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(new HashSet<>(Arrays.asList(userMember, adminMember)));
        when(userService.findById(admin.getId())).thenReturn(admin);
        when(userService.findById(user.getId())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(admin.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(user.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
//        when(userService.findByUsername(anyString(), eq(false))).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));
        PageEntity existingPage = mock(PageEntity.class);
        when(pageService.search(any())).thenReturn(Collections.singletonList(existingPage), Collections.emptyList());

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, times(1)).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(pageService, times(1)).update(any(), any(UpdatePageEntity.class));
        verify(membershipRepository, never()).create(any());
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithOnlyDefinition() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
//        when(userService.findByUsername(anyString(), eq(false))).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, never()).create(any());
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithPlans() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        PlanEntity existingPlan = new PlanEntity();
        when(planService.search(any())).thenReturn(Collections.singletonList(existingPlan), Collections.emptyList());

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(planService, times(1)).create(any(NewPlanEntity.class));
        verify(planService, times(1)).update(any(UpdatePlanEntity.class));
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }
}
