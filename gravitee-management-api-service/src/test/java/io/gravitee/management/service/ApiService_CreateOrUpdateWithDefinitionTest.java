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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_CreateOrUpdateWithDefinitionTest {

    private static final String API_ID = "id-api";
    private static final String PLAN_ID = "my-plan";

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

    @Test
    public void shouldUpdateImportApiWithMembersAndPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(owner));
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, times(2)).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name());
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                RoleScope.API,
                "OWNER");
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    private ApiEntity prepareUpdateImportApiWithMembers(UserEntity admin, UserEntity user) throws IOException, TechnicalException {
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(owner));

        admin.setUsername(po.getUserId());

        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);
        return apiEntity;
    }

    @Test
    public void shouldUpdateImportApiWithMembers() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(Collections.emptySet());

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, never()).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name());
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                RoleScope.API,
                "OWNER");
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndUserAlreadyExists() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        MemberEntity userMember = new MemberEntity();
        userMember.setUsername("user");
        userMember.setRole("OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(Collections.singleton(userMember));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, never()).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name());
        verify(membershipService, never()).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                RoleScope.API,
                "OWNER");
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithMembersAndAllMembersAlreadyExists() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        UserEntity admin = new UserEntity();
        UserEntity user = new UserEntity();
        ApiEntity apiEntity = prepareUpdateImportApiWithMembers(admin, user);
        MemberEntity userMember = new MemberEntity();
        userMember.setUsername("user");
        userMember.setRole("OWNER");
        MemberEntity adminMember = new MemberEntity();
        adminMember.setUsername("admin");
        adminMember.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(new HashSet<>(Arrays.asList(userMember, adminMember)));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, "import");

        verify(pageService, never()).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name());
        verify(membershipService, never()).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                RoleScope.API,
                "OWNER");
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(userService.findByName(anyString(), eq(false))).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, times(2)).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, never()).create(any());
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithOnlyDefinition() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(userService.findByName(anyString(), eq(false))).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, never()).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, never()).create(any());
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldCreateImportApiWithMembersAndPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(userService.findByName(anyString(), eq(false))).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name());
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                RoleScope.API,
                "OWNER");
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
    }

    @Test
    public void shouldCreateImportApiWithMembers() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);
        when(groupService.findByEvent(any())).thenReturn(Collections.emptySet());

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name());
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                RoleScope.API,
                "OWNER");
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
    }

    @Test
    public void shouldCreateImportApiWithPages() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+pages.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());

    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinition() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).createApiPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
    }

    @Test
    public void shouldCreateImportApiWithPlans() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+plans.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername(), false)).thenReturn(admin);
        when(userService.findByName(user.getUsername(), false)).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(planService, times(1)).create(any(NewPlanEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());

    }
}
