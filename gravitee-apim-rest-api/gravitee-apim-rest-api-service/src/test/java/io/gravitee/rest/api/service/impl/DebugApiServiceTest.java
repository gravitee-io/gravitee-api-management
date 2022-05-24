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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Plan;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.DebugApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DebugApiInvalidDefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.DebugApiNoCompatibleInstanceException;
import io.gravitee.rest.api.service.exceptions.DebugApiNoValidPlanException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DebugApiServiceTest {

    private static final String API_ID = "api#1";
    private static final String ENVIRONMENT_ID = "environment#1";
    private static final String INSTANCE_ID = "instance#1";
    private static final String USER_ID = "user#1";
    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiService apiService;

    @Mock
    private EventService eventService;

    @Mock
    private InstanceService instanceService;

    private DebugApiService debugApiService;

    @Before
    public void setup() {
        debugApiService = new DebugApiServiceImpl(apiService, eventService, objectMapper, instanceService);

        ApiEntity apiEntity = mock(ApiEntity.class);
        when(apiEntity.getReferenceId()).thenReturn(ENVIRONMENT_ID);
        when(apiService.findById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiEntity);

        when(instanceService.findAllStarted(GraviteeContext.getExecutionContext())).thenReturn(List.of(getAnInstanceEntity()));
    }

    @Test
    public void debug_shouldCallEventServiceWithSpecificProperties() {
        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V2);

        debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);

        ArgumentCaptor<Map<String, String>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventService)
            .createDebugApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(EventType.DEBUG_API),
                any(),
                propertiesCaptor.capture()
            );
        verify(apiService, times(1)).checkPolicyConfigurations(anyMap(), anyList(), anyList());

        assertThat(propertiesCaptor.getValue())
            .contains(
                entry(Event.EventProperties.USER.getValue(), USER_ID),
                entry(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
            );
    }

    @Test
    public void debug_shouldCallEventServiceWithYoungestGatewayInstance() {
        InstanceEntity youngInstance = getAnInstanceEntity();
        youngInstance.setId("instance#young");
        youngInstance.setStartedAt(new Date(5000));

        InstanceEntity oldInstance = getAnInstanceEntity();
        oldInstance.setId("instance#old");
        oldInstance.setStartedAt(new Date(1000));

        when(instanceService.findAllStarted(GraviteeContext.getExecutionContext())).thenReturn(List.of(youngInstance, oldInstance));

        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V2);
        debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);

        ArgumentCaptor<Map<String, String>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventService)
            .createDebugApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(EventType.DEBUG_API),
                any(),
                propertiesCaptor.capture()
            );
        verify(apiService, times(1)).checkPolicyConfigurations(anyMap(), anyList(), anyList());

        assertThat(propertiesCaptor.getValue()).contains(entry(Event.EventProperties.GATEWAY_ID.getValue(), "instance#young"));
    }

    @Test(expected = DebugApiNoValidPlanException.class)
    public void debug_shouldThrowIfApiHasOnlyDeprecatedPlan() {
        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.DEPRECATED, DefinitionVersion.V2);

        debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);
    }

    @Test(expected = DebugApiInvalidDefinitionVersionException.class)
    public void debug_shouldThrowIfApiDefinitionIsNotV2() {
        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V1);

        debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);
    }

    @Test(expected = DebugApiNoCompatibleInstanceException.class)
    public void debug_shouldThrowIfNoGatewayWithCorrectTag() {
        InstanceEntity instanceEntity = getAnInstanceEntity();
        instanceEntity.setTags(List.of("internal"));

        when(instanceService.findAllStarted(GraviteeContext.getExecutionContext())).thenReturn(List.of(instanceEntity));

        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V2);
        debugApiEntity.setTags(Set.of("external"));
        debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);
    }

    @Test(expected = DebugApiNoCompatibleInstanceException.class)
    public void debug_shouldThrowIfNoGatewayWithDebugPlugin() {
        InstanceEntity instanceEntity = getAnInstanceEntity();

        PluginEntity pluginEntity = new PluginEntity();
        pluginEntity.setId("rate-limit");
        instanceEntity.setPlugins(Set.of(pluginEntity));

        when(instanceService.findAllStarted(GraviteeContext.getExecutionContext())).thenReturn(List.of(instanceEntity));

        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V2);
        debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);
    }

    @Test
    public void debug_shouldNotCreateDebugEventIfPoliciesConfigurationAreInvalid() {
        InstanceEntity youngInstance = getAnInstanceEntity();
        youngInstance.setId("instance#young");
        youngInstance.setStartedAt(new Date(5000));

        InstanceEntity oldInstance = getAnInstanceEntity();
        oldInstance.setId("instance#old");
        oldInstance.setStartedAt(new Date(1000));

        doThrow(new InvalidDataException("Unable to validate policy configuration"))
            .when(apiService)
            .checkPolicyConfigurations(anyMap(), anyList(), anyList());

        DebugApiEntity debugApiEntity = prepareDebugApiEntity(PlanStatus.PUBLISHED, DefinitionVersion.V2);
        try {
            debugApiService.debug(GraviteeContext.getExecutionContext(), API_ID, USER_ID, debugApiEntity);
        } catch (InvalidDataException e) {
            assertNotNull(e);
        }
        ArgumentCaptor<Map<String, String>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventService, times(0))
            .createDebugApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(EventType.DEBUG_API),
                any(),
                propertiesCaptor.capture()
            );
        verify(apiService, times(1)).checkPolicyConfigurations(anyMap(), anyList(), anyList());
    }

    private DebugApiEntity prepareDebugApiEntity(PlanStatus planStatus, DefinitionVersion definitionVersion) {
        Plan deprecatedPlan = new Plan();
        deprecatedPlan.setStatus(planStatus.name());

        DebugApiEntity debugApiEntity = new DebugApiEntity();
        debugApiEntity.setRequest(new HttpRequest());
        debugApiEntity.setGraviteeDefinitionVersion(definitionVersion.getLabel());
        debugApiEntity.setPlans(List.of(deprecatedPlan));

        return debugApiEntity;
    }

    private InstanceEntity getAnInstanceEntity() {
        InstanceEntity instanceEntity = new InstanceEntity();
        instanceEntity.setId(INSTANCE_ID);
        instanceEntity.setEnvironments(Sets.newSet(ENVIRONMENT_ID));

        PluginEntity pluginEntity = new PluginEntity();
        pluginEntity.setId("gateway-debug");

        instanceEntity.setPlugins(Set.of(pluginEntity));

        return instanceEntity;
    }
}
