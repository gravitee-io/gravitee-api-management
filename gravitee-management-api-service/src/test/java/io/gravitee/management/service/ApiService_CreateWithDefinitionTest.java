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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.management.service.spring.ServiceConfiguration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
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
import java.util.Collections;
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
    private GenericNotificationConfigService  genericNotificationConfigService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private AlertService alertService;

    @Before
    public void init() {
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);

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
        when(userService.findBySource(anyString(), anyString(), eq(false))).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership owner = new Membership("user", API_ID, MembershipReferenceType.API);
        owner.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(admin.getId())).thenReturn(admin);

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(Collections.singleton(memberEntity));
        when(membershipService.addOrUpdateMember(any(), any(), any())).thenReturn(memberEntity);
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(memberEntity.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(admin.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(user.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
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
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findBySource(user.getSource(), user.getSourceId(), false)).thenReturn(user);
        when(userService.findById(admin.getId())).thenReturn(admin);
        when(groupService.findByEvent(any())).thenReturn(Collections.emptySet());

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(admin.getId());
        memberEntity.setRole("PRIMARY_OWNER");
        when(membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API)).thenReturn(Collections.singleton(memberEntity));
        when(membershipService.addOrUpdateMember(any(), any(), any())).thenReturn(memberEntity);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipService, never()).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(admin.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));
        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipUser(user.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(membershipService, never()).transferApiOwnership(any(), any(), any());
        verify(genericNotificationConfigService, times(1)).create(any());
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
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId("ref-admin");
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId("ref-user");
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, times(2)).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
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
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
    }

    @Test
    public void shouldCreateImportApiWithOnlyDefinitionEnumLowerCase() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition_enum_lowercase.json");
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
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(pageService, never()).createPage(eq(API_ID), any(NewPageEntity.class));
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
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(planService, times(2)).create(any(NewPlanEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
    }

    @Test
    public void shouldCreateImportApiWithMetadata() throws IOException, TechnicalException {
        URL url =  Resources.getResource("io/gravitee/management/service/import-api.definition+metadata.json");
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
        UserEntity admin = new UserEntity();
        admin.setId(po.getUserId());
        admin.setSource(SOURCE);
        admin.setSourceId(po.getReferenceId());
        UserEntity user = new UserEntity();
        user.setId(owner.getUserId());
        user.setSource(SOURCE);
        user.setSourceId(owner.getReferenceId());
        when(userService.findById(admin.getId())).thenReturn(admin);

        apiService.createOrUpdateWithDefinition(null, toBeImport, "admin");

        verify(apiMetadataService, times(1)).create(any(NewApiMetadataEntity.class));
        verify(apiMetadataService, times(2)).update(any(UpdateApiMetadataEntity.class));
        verify(membershipRepository, times(1)).create(po);
        verify(apiRepository, never()).update(any());
        verify(apiRepository, times(1)).create(any());
        verify(genericNotificationConfigService, times(1)).create(any());
    }
}
