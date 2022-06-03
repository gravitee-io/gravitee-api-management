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

import static io.gravitee.definition.model.DefinitionVersion.V1;
import static io.gravitee.definition.model.DefinitionVersion.V2;
import static io.gravitee.repository.management.model.ApiLifecycleState.DEPRECATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_CREATED;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.NewPlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.ApiDeprecatedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_CreateTest {

    private static final String API_ID = "my-api";

    @Spy
    @InjectMocks
    private PlanServiceImpl planService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private NewPlanEntity newPlanEntity;

    @Mock
    private Plan plan;

    @Mock
    private Api api;

    @Before
    public void setup() throws Exception {
        when(newPlanEntity.getApi()).thenReturn(API_ID);
        when(newPlanEntity.getSecurity()).thenReturn(PlanSecurityType.KEY_LESS);
        when(api.getDefinition()).thenReturn("apidefinition");

        when(parameterService.findAsBoolean(any(), any(), any())).thenReturn(true);
        when(planRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);
        when(planConverter.toPlan(any(NewPlanEntity.class), any())).thenReturn(plan);

        mockApiDefinitionVersion(V2);
    }

    @Test(expected = ApiNotFoundException.class)
    public void should_throw_apiNotFoundException_cause_api_not_found() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);
    }

    @Test(expected = ApiDeprecatedException.class)
    public void should_throw_apiDeprecatedException_cause_api_found_is_deprecated() throws Exception {
        when(api.getApiLifecycleState()).thenReturn(DEPRECATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);
        verifyNoMoreInteractions(planConverter);
    }

    @Test
    public void should_convert_using_api_definition_version_v2() throws Exception {
        mockApiDefinitionVersion(V2);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);

        verify(planConverter, times(1)).toPlan(newPlanEntity, V2);
    }

    @Test
    public void should_convert_using_api_definition_version_v1() throws Exception {
        mockApiDefinitionVersion(V1);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);

        verify(planConverter, times(1)).toPlan(newPlanEntity, V1);
    }

    @Test
    public void should_create_audit_log() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);

        verify(auditService, times(1))
            .createApiAuditLog(eq(GraviteeContext.getExecutionContext()), eq(API_ID), any(), eq(PLAN_CREATED), any(), isNull(), same(plan));
        verifyNoMoreInteractions(auditService);
    }

    // mock object mapper in order to deserialize api definition version
    private void mockApiDefinitionVersion(DefinitionVersion version) throws Exception {
        var apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setDefinitionVersion(version);
        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class)).thenReturn(apiDefinition);
    }
}
