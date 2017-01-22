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
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
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
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findByReferencesAndMembershipType(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                MembershipType.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(owner));
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, times(2)).create(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                MembershipType.valueOf(po.getType()));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                MembershipType.valueOf(owner.getType()));
        verify(apiRepository, times(1)).update(any());
        verify(apiRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateImportApiWithMembers() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+members.json");
        String toBeImport = Resources.toString(url, Charsets.UTF_8);
        ApiEntity apiEntity = new ApiEntity();
        Api api = new Api();
        api.setId(API_ID);
        apiEntity.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findByReferencesAndMembershipType(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                MembershipType.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(owner));
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, never()).create(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                MembershipType.valueOf(po.getType()));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                MembershipType.valueOf(owner.getType()));
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
        when(userService.findByName(anyString())).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferencesAndMembershipType(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                MembershipType.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, times(2)).create(eq(API_ID), any(NewPageEntity.class));
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
        when(userService.findByName(anyString())).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferencesAndMembershipType(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                MembershipType.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        apiService.createOrUpdateWithDefinition(apiEntity, toBeImport, null);

        verify(pageService, never()).create(eq(API_ID), any(NewPageEntity.class));
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
        when(userService.findByName(anyString())).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).create(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                MembershipType.valueOf(po.getType()));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                MembershipType.valueOf(owner.getType()));
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
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).create(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                admin.getUsername(),
                MembershipType.valueOf(po.getType()));
        verify(membershipService, times(1)).addOrUpdateMember(
                MembershipReferenceType.API,
                API_ID,
                user.getUsername(),
                MembershipType.valueOf(owner.getType()));
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
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).create(eq(API_ID), any(NewPageEntity.class));
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
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).create(eq(API_ID), any(NewPageEntity.class));
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
        po.setType(MembershipType.PRIMARY_OWNER.name());
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setType(MembershipType.OWNER.name());
        when(membershipRepository.findById(po.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.of(po));
        when(membershipRepository.findById(owner.getUserId(), MembershipReferenceType.API, API_ID)).thenReturn(Optional.empty());
        UserEntity admin = new UserEntity();
        admin.setUsername(po.getUserId());
        UserEntity user = new UserEntity();
        user.setUsername(owner.getUserId());
        when(userService.findByName(admin.getUsername())).thenReturn(admin);
        when(userService.findByName(user.getUsername())).thenReturn(user);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(planService, times(1)).create(any(NewPlanEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());

    }
}
