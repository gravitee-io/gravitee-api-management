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
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.flow.FlowEntity;
import io.gravitee.rest.api.service.ApiDefinitionService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiDefinitionServiceImplTest {

    private ApiDefinitionService cut;
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private PlanConverter planConverter;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        cut = new ApiDefinitionServiceImpl(objectMapper, planService, planConverter, flowService);
    }

    @Test
    @DisplayName("Should build gateway API definition")
    public void shouldBuildGatewayApiDefinition() throws JsonProcessingException {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        Api api = new Api();
        api.setId("api-id");
        api.setDefinition(
            "{\"id\":\"34b1d048-ec81-30eb-bdc9-ea2d19f5bf6f\",\"name\":\"my-api\",\"version\":\"1.0\",\"gravitee\":\"2.0.0\",\"execution_mode\":\"v3\",\"flow_mode\":\"DEFAULT\",\"proxy\":{\"virtual_hosts\":[{\"path\":\"/test/\"}],\"strip_context_path\":false,\"preserve_host\":false,\"groups\":[{\"name\":\"default-group\",\"endpoints\":[],\"load_balancing\":{\"type\":\"ROUND_ROBIN\"},\"http\":{\"connectTimeout\":5000,\"idleTimeout\":60000,\"keepAlive\":true,\"readTimeout\":10000,\"pipelining\":false,\"maxConcurrentConnections\":100,\"useCompression\":true,\"followRedirects\":false}}]}}"
        );

        PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId("plan-1");
        closedPlan.setStatus(PlanStatus.CLOSED);

        PlanEntity stagingPlan = new PlanEntity();
        stagingPlan.setId("plan-3");
        stagingPlan.setStatus(PlanStatus.STAGING);

        PlanEntity publishedPlan = new PlanEntity();
        publishedPlan.setId("plan-2");
        publishedPlan.setName("plan-2");
        publishedPlan.setStatus(PlanStatus.PUBLISHED);
        when(planService.findByApi(executionContext, api.getId())).thenReturn(Set.of(closedPlan, publishedPlan));

        FlowEntity flow = new FlowEntity();
        flow.setName("flow-1");
        when(flowService.findByReference(FlowReferenceType.API, api.getId())).thenReturn(List.of(flow));

        Plan plan = new Plan();
        plan.setName("plan-2");
        when(planConverter.toPlansDefinitions(Set.of(publishedPlan))).thenReturn(List.of(plan));

        io.gravitee.definition.model.Api result = cut.buildGatewayApiDefinition(executionContext, api);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("my-api");
        assertThat(result.getPlans()).extracting(Plan::getName).containsExactly("plan-2");
        assertThat(result.getFlows()).extracting(Flow::getName).containsExactly("flow-1");
    }

    @Test
    @DisplayName("Should throw JsonProcessingException")
    public void shouldThrowJsonProcessingException() throws JsonProcessingException {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        Api api = new Api();
        api.setId("api-id");
        api.setDefinition("{ foo: bar }");

        assertThrows(JsonProcessingException.class, () -> cut.buildGatewayApiDefinition(executionContext, api));
    }
}
