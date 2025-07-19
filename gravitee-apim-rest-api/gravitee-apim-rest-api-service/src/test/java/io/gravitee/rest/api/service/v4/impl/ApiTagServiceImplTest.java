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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiTagService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiTagServiceImplTest {

    @Mock
    EnvironmentService environmentService;

    @Mock
    ApiRepository apiRepository;

    @Mock
    AuditService auditService;

    @Mock
    ApiNotificationService apiNotificationService;

    @Mock
    ObjectMapper objectMapper;

    private ApiTagService apiTagService;

    @Before
    public void before() {
        apiTagService = new ApiTagServiceImpl(apiRepository, environmentService, objectMapper, apiNotificationService, auditService);
    }

    @Test
    public void shouldDeleteTagsFromV2Api() throws TechnicalException, JsonProcessingException {
        final ExecutionContext executionContext = new ExecutionContext("DEFAULT", null);

        final EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("DEFAULT");

        final Api api = new Api();
        api.setId("api-id");
        api.setDefinitionVersion(DefinitionVersion.V2);
        api.setDefinition("{\"tags\": [\"intranet\"]}");

        final io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setTags(new HashSet<>(Set.of("intranet")));

        when(environmentService.findByOrganization("DEFAULT")).thenReturn(List.of(environment));
        when(apiRepository.search(any(), isNull(), isA(ApiFieldFilter.class))).thenReturn(Stream.of(api));
        when(apiRepository.update(any())).then(invocation -> invocation.getArgument(0));

        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class)).thenReturn(apiDefinition);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        apiTagService.deleteTagFromAPIs(executionContext, "intranet");

        verify(apiNotificationService, times(1)).triggerUpdateNotification(any(), any(Api.class));
        verify(auditService, times(1)).createApiAuditLog(any(), any(), any(), any(), any(), any(), any());
        verify(apiRepository, times(1)).update(argThat(apiUpdate -> !apiUpdate.getDefinition().contains("intranet")));
    }

    @Test
    public void shouldDeleteTagsFromV4Api() throws TechnicalException, JsonProcessingException {
        final ExecutionContext executionContext = new ExecutionContext("DEFAULT", null);

        final EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("DEFAULT");

        final Api api = new Api();
        api.setId("api-id");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setDefinition("{\"tags\": [\"intranet\"]}");

        final io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setTags(new HashSet<>(Set.of("intranet")));

        when(environmentService.findByOrganization("DEFAULT")).thenReturn(List.of(environment));
        when(apiRepository.search(any(), isNull(), isA(ApiFieldFilter.class))).thenReturn(Stream.of(api));
        when(apiRepository.update(any())).then(invocation -> invocation.getArgument(0));

        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.AbstractApi.class)).thenReturn(apiDefinition);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        apiTagService.deleteTagFromAPIs(executionContext, "intranet");

        verify(apiRepository, times(1)).update(argThat(apiUpdate -> !apiUpdate.getDefinition().contains("intranet")));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(any(), any(Api.class));
        verify(auditService, times(1)).createApiAuditLog(any(), any(), any(), any(), any(), any(), any());
    }
}
