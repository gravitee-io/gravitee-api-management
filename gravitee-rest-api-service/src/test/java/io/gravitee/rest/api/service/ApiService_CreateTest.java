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
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.NewApiEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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

import java.util.Collections;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private PageService pageService;
    @Mock
    private UserService userService;
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
    public void shouldCreateForUser() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getLifecycleState()).thenReturn(LifecycleState.STARTED);
        when(api.getApiLifecycleState()).thenReturn(ApiLifecycleState.PUBLISHED);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        when(pageService.search(any())).thenReturn(null);

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

        when(apiRepository.search(new ApiCriteria.Builder().environment("DEFAULT").build())).thenReturn(asList(api));
        when(api.getId()).thenReturn(API_ID);
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"" + existingContextPath + "\"}}");

        when(newApi.getContextPath()).thenReturn(contextPathToCreate);
        when(userService.findById(USER_NAME)).thenReturn(new UserEntity());
        when(pageService.search(any())).thenReturn(null);

        apiService.create(newApi, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenThrow(TechnicalException.class);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
//        when(userService.findByUsername(USER_NAME, false)).thenReturn(new UserEntity());

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
        admin.setId(USER_NAME);
        when(userService.findById(admin.getId())).thenReturn(admin);

        final ApiEntity apiEntity = apiService.create(newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        assertNotNull(apiEntity.getPaths());
        /*assertTrue("paths not empty", !apiEntity.getPaths().isEmpty());
        assertEquals("paths.size == 1", apiEntity.getPaths().size(), 1);
        assertEquals("path == /* ", apiEntity.getPaths().get(0).getPath(), "/*");*/
    }
}
