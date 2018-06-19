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
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.api.NewApiEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_CreateTest {

    private static final String API_ID = "id-api";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private NewApiEntity newApi;

    @Mock
    private Api api;

    @Mock
    private GroupService groupService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getLifecycleState()).thenReturn(LifecycleState.STARTED);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());

        when(groupService.findByEvent(any())).thenReturn(Collections.emptySet());

        final ApiEntity apiEntity = apiService.create(newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = ApiAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseExists() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenReturn(Optional.of(api));
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");

        apiService.create(newApi, USER_NAME);
    }

    @Test
    public void shouldCreateForUserBecauseContextPathNotExists() throws TechnicalException {
        testCreationWithContextPath("/context", "/context2");
    }

    @Test
    public void shouldCreateForUserBecauseContextPathNotExists2() throws TechnicalException {
        testCreationWithContextPath("/context2", "/context");
    }

    @Test
    public void shouldCreateForUserBecauseContextPathNotExists3() throws TechnicalException {
        testCreationWithContextPath("/products/sect/search", "/products/ecom/search");
    }

    @Test
    public void shouldCreateForUserBecauseContextPathNotExists4() throws TechnicalException {
        testCreationWithContextPath("/products/sect/search", "/products/ecom");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseContextPathExists() throws TechnicalException {
        testCreationWithContextPath("/context", "/context");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseSubContextPathExists() throws TechnicalException {
        testCreationWithContextPath("/context/toto", "/context");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseSubContextPathExists2() throws TechnicalException {
        testCreationWithContextPath("/context", "/context/toto");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseSubContextPathExists3() throws TechnicalException {
        testCreationWithContextPath("/products/sect/search", "/products/sect");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseContextPathExists_TrailingSlash() throws TechnicalException {
        testCreationWithContextPath("/context/", "/context/toto");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseContextPathExists_TrailingSlash2() throws TechnicalException {
        testCreationWithContextPath("/context//toto", "/context/toto");
    }

    private void testCreationWithContextPath(String existingContextPath, String contextPathToCreate) throws TechnicalException {
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");

        when(apiRepository.search(null)).thenReturn(asList(api));
        when(api.getId()).thenReturn(API_ID);
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"" + existingContextPath + "\"}}");

        when(newApi.getContextPath()).thenReturn(contextPathToCreate);
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        Membership po = new Membership("admin", API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                Collections.singletonList(API_ID),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po));

        apiService.create(newApi, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenThrow(TechnicalException.class);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(userService.findByUsername(USER_NAME, false)).thenReturn(new UserEntity());

        apiService.create(newApi, USER_NAME);
    }
    @Test
    public void shouldCreateWithDefaultPath() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");
        UserEntity admin = new UserEntity();
        admin.setUsername(USER_NAME);
        when(userService.findById(admin.getUsername())).thenReturn(admin);

        final ApiEntity apiEntity = apiService.create(newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        assertNotNull(apiEntity.getPaths());
        /*assertTrue("paths not empty", !apiEntity.getPaths().isEmpty());
        assertEquals("paths.size == 1", apiEntity.getPaths().size(), 1);
        assertEquals("path == /* ", apiEntity.getPaths().get(0).getPath(), "/*");*/
    }
}
