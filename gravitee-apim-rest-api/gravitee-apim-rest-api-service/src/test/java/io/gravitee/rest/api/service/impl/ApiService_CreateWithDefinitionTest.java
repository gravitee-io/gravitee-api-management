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

import static io.gravitee.definition.model.DefinitionContext.*;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.validation.LoggingValidationService;
import java.io.InputStream;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_CreateWithDefinitionTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private VirtualHostService virtualHostService;

    @Mock
    private ConnectorService connectorService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private UserService userService;

    @Mock
    private JupiterModeService jupiterModeService;

    @Mock
    private GroupService groupService;

    @Mock
    private AuditService auditService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GenericNotificationConfigService notificationConfigService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private FlowService flowService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlanService planService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private LoggingValidationService loggingValidationService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @InjectMocks
    private final ApiService apiService = new ApiServiceImpl();

    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();

    private static final ObjectMapper MAPPER = new GraviteeMapper();

    @Test
    public void shouldStartApiIfManagedByKubernetes() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-new-kubernetes-api.definition.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = new Endpoint("endpointName", null);
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("k8s basic");
        api.setDescription("k8s basic example");
        api.setDefinitionContext(new DefinitionContext(ORIGIN_KUBERNETES, MODE_FULLY_MANAGED));

        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));

        Api updateApiDefinition = new Api();
        when(objectMapper.readValue(any(String.class), eq(Api.class))).thenReturn(updateApiDefinition);

        when(apiRepository.create(any())).thenReturn(new io.gravitee.repository.management.model.Api());

        when(apiConverter.toApiEntity(any(), any())).thenReturn(new ApiEntity());

        when(apiMetadataService.fetchMetadataForApi(any(), any())).thenReturn(new ApiEntity());

        apiService.createWithApiDefinition(EXECUTION_CONTEXT, api, "", definition);

        verify(apiRepository, times(1)).create(argThat(arg -> arg.getLifecycleState().equals(LifecycleState.STARTED)));
    }

    @Test
    public void shouldNotStartApiIfNotManagedByKubernetes() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-new-kubernetes-api.definition.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = new Endpoint("endpointName", null);
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("k8s basic");
        api.setDescription("k8s basic example");
        api.setDefinitionContext(new DefinitionContext(ORIGIN_MANAGEMENT, MODE_FULLY_MANAGED));

        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));

        Api updateApiDefinition = new Api();
        when(objectMapper.readValue(any(String.class), eq(Api.class))).thenReturn(updateApiDefinition);

        when(apiRepository.create(any())).thenReturn(new io.gravitee.repository.management.model.Api());

        when(apiConverter.toApiEntity(any(), any())).thenReturn(new ApiEntity());

        when(apiMetadataService.fetchMetadataForApi(any(), any())).thenReturn(new ApiEntity());

        apiService.createWithApiDefinition(EXECUTION_CONTEXT, api, "", definition);

        verify(apiRepository, times(1)).create(argThat(arg -> arg.getLifecycleState().equals(LifecycleState.STOPPED)));
    }

    private JsonNode readDefinition(String resourcePath) throws Exception {
        InputStream resourceAsStream = getClass().getResourceAsStream(resourcePath);
        return MAPPER.readTree(resourceAsStream);
    }
}
