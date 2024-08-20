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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.notification.ApiHook;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GroupService_AssociateTest extends TestCase {

    private static final String GROUP_ID = "my-group-id";

    @InjectMocks
    private final GroupService groupService = new GroupServiceImpl();

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NotifierService notifierService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private EventManager eventManager;

    @Test(expected = GroupNotFoundException.class)
    public void shouldThrowGroupNotFoundException() throws TechnicalException {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());
        groupService.findById(null, GROUP_ID);
    }

    @Test
    public void shouldAssociateAllApi() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        Api api1 = new Api();
        api1.setId("api1");
        Api api2 = new Api();
        api2.setId("api2");
        api2.setGroups(Set.of(GROUP_ID));

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build(),
                null,
                ApiFieldFilter.allFields()
            )
        )
            .thenReturn(Stream.of(api1, api2));
        ApiEntity apiEntity1 = new ApiEntity();
        apiEntity1.setId("api1");
        when(apiConverter.toApiEntity(api1, null)).thenReturn(apiEntity1);

        UserEntity fakeUserEntity = new UserEntity();
        fakeUserEntity.setFirstname("firstName");
        fakeUserEntity.setLastname("lastName");
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeUserEntity);

        groupService.associate(executionContext, GROUP_ID, "api");

        verify(apiRepository, times(1)).update(api1);
        verify(notifierService, times(1)).trigger(eq(executionContext), eq(ApiHook.API_UPDATED), eq(api1.getId()), any());
    }

    @Test
    public void shouldAssociateAllApplication() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        Application app1 = new Application();
        app1.setId("api1");
        Application app2 = new Application();
        app2.setId("api2");
        app2.setGroups(Set.of(GROUP_ID));

        when(applicationRepository.findAllByEnvironment(executionContext.getEnvironmentId())).thenReturn(Set.of(app1, app2));

        groupService.associate(executionContext, GROUP_ID, "application");

        verify(applicationRepository, times(1)).update(app1);
        verify(eventManager, times(1)).publishEvent(eq(ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE), any());
    }
}
