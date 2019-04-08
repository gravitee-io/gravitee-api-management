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
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Proxy;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.api.UpdateApiEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.management.service.search.SearchEngineService;
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
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_UpdateTest {

    private static final String API_ID = "id-api";
    private static final String API_ID2 = "id-api2";
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
    private UpdateApiEntity existingApi;
    @Mock
    private Api api;
    @Mock
    private UserService userService;
    @Mock
    private AuditService auditService;
    @Mock
    private SearchEngineService searchEngineService;
    @Mock
    private TagService tagService;

    @Mock
    private ParameterService parameterService;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));

        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        prepareUpdate();

        final ApiEntity apiEntity = apiService.update(API_ID, existingApi);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

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

        when(apiRepository.search(null)).thenReturn(singletonList(api));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"" + existingContextPath + "\"}}");

        Membership po2 = new Membership("admin", API_ID2, MembershipReferenceType.API);
        po2.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                singletonList(API_ID2),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(po2));

        apiService.update(API_ID, existingApi);
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldNotUpdateWithInvalidEndpointGroupName() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(api.getId()).thenReturn(API_ID2);
        when(api.getName()).thenReturn(API_NAME);

        final Proxy proxy = mock(Proxy.class);
        when(existingApi.getProxy()).thenReturn(proxy);
        when(proxy.getContextPath()).thenReturn("/new");
        final EndpointGroup group = mock(EndpointGroup.class);
        when(group.getName()).thenReturn("inva:lid");
        when(proxy.getGroups()).thenReturn(singleton(group));

        when(apiRepository.search(null)).thenReturn(singletonList(api));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}");

        apiService.update(API_ID, existingApi);

        fail("should throw EndpointNameInvalidException");
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldNotUpdateWithInvalidEndpointName() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(api.getId()).thenReturn(API_ID2);
        when(api.getName()).thenReturn(API_NAME);

        final Proxy proxy = mock(Proxy.class);
        when(existingApi.getProxy()).thenReturn(proxy);
        when(proxy.getContextPath()).thenReturn("/new");
        final EndpointGroup group = mock(EndpointGroup.class);
        when(group.getName()).thenReturn("group");
        when(proxy.getGroups()).thenReturn(singleton(group));
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getName()).thenReturn("inva:lid");
        when(group.getEndpoints()).thenReturn(singleton(endpoint));

        when(apiRepository.search(null)).thenReturn(singletonList(api));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}");

        apiService.update(API_ID, existingApi);

        fail("should throw EndpointNameInvalidException");
    }

    private void prepareUpdate() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(api.getName()).thenReturn(API_NAME);

        when(existingApi.getName()).thenReturn(API_NAME);
        when(existingApi.getVersion()).thenReturn("v1");
        when(existingApi.getDescription()).thenReturn("Ma description");
        final Proxy proxy = mock(Proxy.class);
        when(existingApi.getProxy()).thenReturn(proxy);
        when(proxy.getContextPath()).thenReturn("/context");
        Membership po = new Membership(USER_NAME, API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(po));
    }

    @Test
    public void shouldUpdateWithAllowedTag() throws TechnicalException {
        prepareUpdate();
        when(existingApi.getTags()).thenReturn(singleton("public"));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}");
        final ApiEntity apiEntity = apiService.update(API_ID, existingApi);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test
    public void shouldUpdateWithExistingAllowedTag() throws TechnicalException {
        prepareUpdate();
        when(existingApi.getTags()).thenReturn(singleton("private"));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}");
        when(tagService.findByUser(any())).thenReturn(Sets.newSet("public", "private"));
        final ApiEntity apiEntity = apiService.update(API_ID, existingApi);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test
    public void shouldUpdateWithExistingAllowedTags() throws TechnicalException {
        prepareUpdate();
        when(existingApi.getTags()).thenReturn(newSet("public", "private"));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}");
        when(tagService.findByUser(any())).thenReturn(Sets.newSet("public", "private"));
        final ApiEntity apiEntity = apiService.update(API_ID, existingApi);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test
    public void shouldUpdateWithExistingNotAllowedTag() throws TechnicalException {
        prepareUpdate();
        when(existingApi.getTags()).thenReturn(newSet("public", "private"));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\", \"private\"]}");
        final ApiEntity apiEntity = apiService.update(API_ID, existingApi);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = TagNotAllowedException.class)
    public void shouldNotUpdateWithNotAllowedTag() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}");
        when(existingApi.getTags()).thenReturn(singleton("private"));
        when(tagService.findByUser(any())).thenReturn(emptySet());
        apiService.update(API_ID, existingApi);
    }

    @Test(expected = TagNotAllowedException.class)
    public void shouldNotUpdateWithExistingNotAllowedTag() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}");
        when(existingApi.getTags()).thenReturn(singleton("private"));
        when(tagService.findByUser(any())).thenReturn(singleton("public"));
        apiService.update(API_ID, existingApi);
    }

    @Test(expected = TagNotAllowedException.class)
    public void shouldNotUpdateWithExistingNotAllowedTags() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(api.getDefinition()).thenReturn("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\", \"private\"]}");
        when(existingApi.getTags()).thenReturn(emptySet());
        when(tagService.findByUser(any())).thenReturn(singleton("private"));
        apiService.update(API_ID, existingApi);
    }
}
