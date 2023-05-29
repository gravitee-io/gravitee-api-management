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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.ApiLifecycleState.DEPRECATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_CREATED;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDeprecatedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
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
    private PlanSearchService planSearchService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

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

    @Mock
    private FlowService flowService;

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private PolicyService policyService;

    @Before
    public void setup() throws Exception {
        when(newPlanEntity.getApiId()).thenReturn(API_ID);
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType(PlanSecurityType.KEY_LESS.getLabel());
        when(newPlanEntity.getSecurity()).thenReturn(planSecurity);
        when(newPlanEntity.getFlows()).thenReturn(new ArrayList<>());
        when(api.getDefinition()).thenReturn("apidefinition");

        when(parameterService.findAsBoolean(any(), any(), any())).thenReturn(true);
        when(planRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);
        when(planMapper.toRepository(any(NewPlanEntity.class))).thenReturn(plan);

        mockApiDefinitionVersion();
    }

    @Test(expected = ApiNotFoundException.class)
    public void should_throw_apiNotFoundException_cause_api_not_found() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);
    }

    @Test(expected = TagNotAllowedException.class)
    public void should_throw_tagNotAllowException_when_tag_validation_fails() throws Exception {
        mockApiDefinitionVersion();
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(this.newPlanEntity.getTags()).thenReturn(Set.of("tag1"));
        doThrow(new TagNotAllowedException(new String[0]))
            .when(tagsValidationService)
            .validatePlanTagsAgainstApiTags(any(), any(Api.class));
        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);
    }

    @Test(expected = ApiDeprecatedException.class)
    public void should_throw_apiDeprecatedException_cause_api_found_is_deprecated() throws Exception {
        when(api.getApiLifecycleState()).thenReturn(DEPRECATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);
        verifyNoMoreInteractions(planMapper);
    }

    @Test(expected = InvalidDataException.class)
    public void should_throw_invalid_data_exception_when_security_configuration_is_invalid() throws Exception {
        final String securityConfiguration = "{ \"foo\": \"bar\"}";
        final PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType(PlanSecurityType.OAUTH2.getLabel());
        planSecurity.setConfiguration(securityConfiguration);

        when(newPlanEntity.getSecurity()).thenReturn(planSecurity);
        when(policyService.validatePolicyConfiguration("oauth2", securityConfiguration))
            .thenThrow(new InvalidDataException("Mock exception"));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);
    }

    @Test
    public void should_convert_using_api_definition() throws Exception {
        mockApiDefinitionVersion();
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);

        verify(planMapper, times(1)).toRepository(newPlanEntity);
    }

    @Test
    public void should_save_plans_flows() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.create(GraviteeContext.getExecutionContext(), this.newPlanEntity);

        verify(flowService, times(1)).save(FlowReferenceType.PLAN, newPlanEntity.getId(), newPlanEntity.getFlows());
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
    private void mockApiDefinitionVersion() throws Exception {
        var apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setDefinitionVersion(DefinitionVersion.V4);
        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class)).thenReturn(apiDefinition);
    }
}
