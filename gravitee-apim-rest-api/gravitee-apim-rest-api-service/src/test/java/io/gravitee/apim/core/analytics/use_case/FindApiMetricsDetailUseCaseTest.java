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
package io.gravitee.apim.core.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fakes.FakeAnalyticsQueryService;
import fakes.FakeInstanceService;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.query_service.gateway.InstanceQueryServiceLegacyWrapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.v4.analytics.ApiMetricsDetail;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindApiMetricsDetailUseCaseTest {

    private static final String API_ID = "api-id";
    private static final String REQUEST_ID = "request-id";
    private static final String APP_ID = "app-id";
    private static final String PLAN_ID = "plan-id";

    FakeAnalyticsQueryService fakeAnalyticsQueryService = new FakeAnalyticsQueryService();
    ApplicationCrudServiceInMemory applicationCrudServiceInMemory = new ApplicationCrudServiceInMemory();
    PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
    FakeInstanceService fakeInstanceService = new FakeInstanceService();

    FindApiMetricsDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
            new FindApiMetricsDetailUseCase(
                fakeAnalyticsQueryService,
                applicationCrudServiceInMemory,
                planCrudServiceInMemory
            );
    }

    @AfterEach
    void tearDown() {
        fakeAnalyticsQueryService.apiMetricsDetail = null;
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_empty_api_analytic() {
        var result = useCase.execute(GraviteeContext.getExecutionContext(), new FindApiMetricsDetailUseCase.Input(API_ID, REQUEST_ID));
        assertThat(result).isNotNull();
        assertThat(result.apiMetricsDetail()).isEmpty();
    }

    @Test
    void should_return_api_analytic_with_unknow_app_and_plan() {
        var applicationId = "unknown-app-id";
        var planId = "unknown-plan-id";
        fakeAnalyticsQueryService.apiMetricsDetail =
            ApiMetricsDetail.builder().apiId(API_ID).applicationId(applicationId).planId(planId).build();

        var result = useCase.execute(GraviteeContext.getExecutionContext(), new FindApiMetricsDetailUseCase.Input(API_ID, REQUEST_ID));

        assertThat(result).isNotNull();
        assertThat(result.apiMetricsDetail())
            .hasValueSatisfying(apiMetricsDetail -> {
                assertThat(apiMetricsDetail.getApplication())
                    .extracting(BaseApplicationEntity::getId, BaseApplicationEntity::getName)
                    .containsExactly(applicationId, "Unknown");

                assertThat(apiMetricsDetail.getPlan())
                    .extracting(GenericPlanEntity::getId, GenericPlanEntity::getName)
                    .containsExactly(planId, "Unknown");
            });
    }

    @Test
    void should_return_an_api_analytic() {
        var appName = "app-name";
        applicationCrudServiceInMemory.initWith(List.of(BaseApplicationEntity.builder().id(APP_ID).name(appName).build()));

        var planName = "plan-name";
        planCrudServiceInMemory.initWith(
            List.of(Plan.builder().id(PLAN_ID).definitionVersion(DefinitionVersion.V4).name(planName).build())
        );

        var instanceId = "instance-id";
        var hostname = "foo.example.com";
        var ip = "42.42.42.1";
        fakeInstanceService.instanceEntity =
            io.gravitee.rest.api.model.InstanceEntity.builder().id(instanceId).hostname(hostname).ip(ip).build();

        var transactionId = "transaction-id";
        var host = "request.host.example.com";
        var uri = "/example/api";
        var status = 200;
        var requestContentLength = 100;
        var responseContentLength = 200;
        var gatewayLatency = 300;
        var gatewayResponseTime = 400;
        var endpointResponseTime = 100;
        var remoteAddress = "192.168.1.1";
        var endpoint = "https://endpoint.example.com/foo";
        fakeAnalyticsQueryService.apiMetricsDetail =
            ApiMetricsDetail
                .builder()
                .apiId(API_ID)
                .requestId(REQUEST_ID)
                .applicationId(APP_ID)
                .planId(PLAN_ID)
                .transactionId(transactionId)
                .host(host)
                .uri(uri)
                .status(status)
                .requestContentLength(requestContentLength)
                .responseContentLength(responseContentLength)
                .gatewayLatency(gatewayLatency)
                .gatewayResponseTime(gatewayResponseTime)
                .gateway(instanceId)
                .remoteAddress(remoteAddress)
                .method(HttpMethod.GET)
                .endpointResponseTime(endpointResponseTime)
                .endpoint(endpoint)
                .build();

        var result = useCase.execute(GraviteeContext.getExecutionContext(), new FindApiMetricsDetailUseCase.Input(API_ID, REQUEST_ID));

        assertThat(result).isNotNull();
        assertThat(result.apiMetricsDetail())
            .hasValueSatisfying(apiMetricsDetail -> {
                assertThat(apiMetricsDetail.getApiId()).isEqualTo(API_ID);
                assertThat(apiMetricsDetail.getRequestId()).isEqualTo(REQUEST_ID);
                assertThat(apiMetricsDetail.getTransactionId()).isEqualTo(transactionId);
                assertThat(apiMetricsDetail.getHost()).isEqualTo(host);
                assertThat(apiMetricsDetail.getUri()).isEqualTo(uri);
                assertThat(apiMetricsDetail.getStatus()).isEqualTo(status);
                assertThat(apiMetricsDetail.getRequestContentLength()).isEqualTo(requestContentLength);
                assertThat(apiMetricsDetail.getResponseContentLength()).isEqualTo(responseContentLength);
                assertThat(apiMetricsDetail.getGatewayLatency()).isEqualTo(gatewayLatency);
                assertThat(apiMetricsDetail.getGatewayResponseTime()).isEqualTo(gatewayResponseTime);
                assertThat(apiMetricsDetail.getRemoteAddress()).isEqualTo(remoteAddress);
                assertThat(apiMetricsDetail.getMethod()).isEqualTo(HttpMethod.GET);
                assertThat(apiMetricsDetail.getEndpointResponseTime()).isEqualTo(endpointResponseTime);
                assertThat(apiMetricsDetail.getEndpoint()).isEqualTo(endpoint);
                assertThat(apiMetricsDetail.getGateway()).isEqualTo(instanceId);

                assertThat(apiMetricsDetail.getApplication())
                    .extracting(BaseApplicationEntity::getId, BaseApplicationEntity::getName)
                    .containsExactly(APP_ID, appName);

                assertThat(apiMetricsDetail.getPlan())
                    .extracting(GenericPlanEntity::getId, GenericPlanEntity::getName)
                    .containsExactly(PLAN_ID, planName);
            });
    }
}
