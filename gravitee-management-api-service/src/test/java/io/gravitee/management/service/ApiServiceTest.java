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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Proxy;
import io.gravitee.management.model.*;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.mixin.ApiMixin;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.Visibility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceTest {

    private static final String API_ID = "id-api";
    private static final String API_ID2 = "id-api2";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private NewApiEntity newApi;
    @Mock
    private UpdateApiEntity existingApi;
    @Mock
    private Api api;
    @Mock
    private EventService eventService;

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

        when(apiRepository.findAll()).thenReturn(new HashSet<>(Arrays.asList(api)));
        when(api.getId()).thenReturn(API_ID);
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"" + existingContextPath + "\"}}");

        when(newApi.getContextPath()).thenReturn(contextPathToCreate);

        apiService.create(newApi, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenThrow(TechnicalException.class);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");

        apiService.create(newApi, USER_NAME);
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final ApiEntity apiEntity = apiService.findById(API_ID);

        assertNotNull(apiEntity);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.findById(API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiService.findById(API_ID);
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        when(apiRepository.findByMember(any(String.class), any(MembershipType.class), any(Visibility.class))).thenReturn(new HashSet<>(Arrays.asList(api)));

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME);

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(apiRepository.findByMember(USER_NAME, null, null)).thenReturn(null);

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByMember(any(String.class), any(MembershipType.class), any(Visibility.class))).thenThrow(TechnicalException.class);

        apiService.findByUser(USER_NAME);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(api.getName()).thenReturn(API_NAME);

        when(existingApi.getName()).thenReturn(API_NAME);
        when(existingApi.getVersion()).thenReturn("v1");
        when(existingApi.getDescription()).thenReturn("Ma description");
        final Proxy proxy = mock(Proxy.class);
        when(existingApi.getProxy()).thenReturn(proxy);
        when(proxy.getContextPath()).thenReturn("/context");

        final ApiEntity apiEntity = apiService.update(API_ID, existingApi);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());
        when(apiRepository.update(any())).thenReturn(api);

        apiService.update(API_ID, existingApi);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(existingApi.getName()).thenReturn(API_NAME);
        when(existingApi.getVersion()).thenReturn("v1");
        when(existingApi.getDescription()).thenReturn("Ma description");
        final Proxy proxy = mock(Proxy.class);
        when(existingApi.getProxy()).thenReturn(proxy);
        when(proxy.getContextPath()).thenReturn("/context");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenThrow(TechnicalException.class);

        apiService.update(API_ID, existingApi);
    }

    @Test
    public void shouldUpdateForUserBecauseContextPathNotExists() throws TechnicalException {
        testUpdateWithContextPath("/context", "/context2");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotUpdateForUserBecauseContextPathExists() throws TechnicalException {
        testUpdateWithContextPath("/context", "/context");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotUpdateForUserBecauseSubContextPathExists() throws TechnicalException {
        testUpdateWithContextPath("/context/toto", "/context");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotUpdateForUserBecauseSubContextPathExists2() throws TechnicalException {
        testUpdateWithContextPath("/context", "/context/toto");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotUpdateForUserBecauseContextPathExistsWithSlash() throws TechnicalException {
        testUpdateWithContextPath("/context", "/context/");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotUpdateForUserBecauseSubContextPathExistsWithSlash() throws TechnicalException {
        testUpdateWithContextPath("/context/toto", "/context/");
    }

    @Test(expected = ApiContextPathAlreadyExistsException.class)
    public void shouldNotUpdateForUserBecauseSubContextPathExists2WithSlash() throws TechnicalException {
        testUpdateWithContextPath("/context", "/context/toto/");
    }

    private void testUpdateWithContextPath(String existingContextPath, String contextPathToCreate) throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(api.getId()).thenReturn(API_ID2);
        when(api.getName()).thenReturn(API_NAME);

        when(existingApi.getName()).thenReturn(API_NAME);
        when(existingApi.getVersion()).thenReturn("v1");
        when(existingApi.getDescription()).thenReturn("Ma description");
        final Proxy proxy = mock(Proxy.class);
        when(existingApi.getProxy()).thenReturn(proxy);
        when(proxy.getContextPath()).thenReturn(contextPathToCreate);

        when(apiRepository.findAll()).thenReturn(new HashSet<>(Arrays.asList(api)));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"" + existingContextPath + "\"}}");

        apiService.update(API_ID, existingApi);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        apiService.delete(API_ID);

        verify(apiRepository).delete(API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiKeyRepository.findByApi(API_ID)).thenReturn(Collections.emptySet());
        doThrow(TechnicalException.class).when(apiRepository).delete(API_ID);

        apiService.delete(API_ID);
    }

    @Test
    public void shouldStart() throws Exception {
        objectMapper.addMixIn(Api.class, ApiMixin.class);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = mockEvent(EventType.PUBLISH_API);
        when(eventService.findByApi(API_ID)).thenReturn(Collections.singleton(event));
        apiService.start(API_ID, USER_NAME);

        verify(api).setUpdatedAt(any());
        verify(api).setLifecycleState(LifecycleState.STARTED);
        verify(apiRepository).update(api);
        verify(eventService).create(EventType.START_API, event.getPayload(), event.getProperties());
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotStartBecauseNotFound() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.start(API_ID, USER_NAME);

        verify(apiRepository, never()).update(api);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotStartBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiService.start(API_ID, USER_NAME);
    }

    @Test
    public void shouldStop() throws Exception {
        objectMapper.addMixIn(Api.class, ApiMixin.class);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = mockEvent(EventType.PUBLISH_API);
        when(eventService.findByApi(API_ID)).thenReturn(Collections.singleton(event));
        apiService.stop(API_ID, USER_NAME);

        verify(api).setUpdatedAt(any());
        verify(api).setLifecycleState(LifecycleState.STOPPED);
        verify(apiRepository).update(api);
        verify(eventService).create(EventType.STOP_API, event.getPayload(), event.getProperties());
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotStopBecauseNotFound() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.stop(API_ID, USER_NAME);

        verify(apiRepository, never()).update(api);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotStopBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiService.stop(API_ID, USER_NAME);
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

        final ApiEntity apiEntity = apiService.create(newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        assertNotNull(apiEntity.getPaths());
        /*assertTrue("paths not empty", !apiEntity.getPaths().isEmpty());
        assertEquals("paths.size == 1", apiEntity.getPaths().size(), 1);
        assertEquals("path == /* ", apiEntity.getPaths().get(0).getPath(), "/*");*/
    }

    private EventEntity mockEvent(EventType eventType) throws Exception {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode node = factory.objectNode();
        node.set("id", factory.textNode(API_ID));

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Event.EventProperties.API_ID.getValue(), API_ID);
        properties.put(Event.EventProperties.USERNAME.getValue(), USER_NAME);

        Api api = new Api();
        api.setId(API_ID);

        EventEntity event = new EventEntity();
        event.setType(eventType);
        event.setId(UUID.randomUUID().toString());
        event.setPayload(objectMapper.writeValueAsString(api));
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());
        event.setProperties(properties);

        return event;
    }
}
