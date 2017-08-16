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
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.mixin.ApiMixin;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.management.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_StartTest {

    private static final String API_ID = "id-api";
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
    private Api api;

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test
    public void shouldStart() throws Exception {
        objectMapper.addMixIn(Api.class, ApiMixin.class);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = mockEvent(EventType.PUBLISH_API);
        when(eventService.findByApi(API_ID)).thenReturn(Collections.singleton(event));
        Membership po = new Membership(USER_NAME, API_ID, MembershipReferenceType.API);
        po.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(po));

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

    private EventEntity mockEvent(EventType eventType) throws Exception {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode node = factory.objectNode();
        node.set("id", factory.textNode(API_ID));

        Map<String, String> properties = new HashMap<>();
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
