/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.mixin.ApiMixin;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_StopTest {

    private static final String API_ID = "id-api";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, mock(WorkflowService.class))
    );

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Before
    public void setUp() throws TechnicalException {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        UserEntity u = new UserEntity();
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(u);
        when(primaryOwnerService.getPrimaryOwner(any(), any())).thenReturn(new PrimaryOwnerEntity(u));
        when(api.getId()).thenReturn(API_ID);
    }

    @Test
    public void shouldStop() throws Exception {
        objectMapper.addMixIn(Api.class, ApiMixin.class);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(api)).thenReturn(api);
        final EventEntity event = mockEvent(PUBLISH_API);
        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        when(eventService.search(GraviteeContext.getExecutionContext(), query)).thenReturn(singleton(event));
        when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(ApiEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(1));

        apiService.stop(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(api).setUpdatedAt(any());
        verify(api).setLifecycleState(LifecycleState.STOPPED);
        verify(apiRepository).update(api);
        verify(eventService)
            .createApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                eq(singleton(GraviteeContext.getCurrentEnvironment())),
                eq(GraviteeContext.getCurrentOrganization()),
                eq(EventType.STOP_API),
                argThat((ArgumentMatcher<Api>) apiArg -> apiArg.getId().equals(API_ID)),
                eq(event.getProperties())
            );
        verify(notifierService, times(1)).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.API_STOPPED), eq(API_ID), any());
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotStopBecauseNotFound() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.stop(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(apiRepository, never()).update(api);
        verify(notifierService, never()).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.API_STOPPED), eq(API_ID), any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotStopBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiService.stop(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);
        verify(notifierService, never()).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.API_STOPPED), eq(API_ID), any());
    }

    private EventEntity mockEvent(EventType eventType) throws Exception {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode node = factory.objectNode();
        node.set("id", factory.textNode(API_ID));

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);

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
