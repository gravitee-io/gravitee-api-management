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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.model.ApiAnalytic;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;

@UseCase
public class SearchApiAnalyticUseCase {

    static final String UNKNOWN = "Unknown";
    private final AnalyticsQueryService analyticsQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final PlanCrudService planCrudService;
    private final InstanceQueryService instanceQueryService;

    public SearchApiAnalyticUseCase(
        AnalyticsQueryService analyticsQueryService,
        ApplicationCrudService applicationCrudService,
        PlanCrudService planCrudService,
        InstanceQueryService instanceQueryService
    ) {
        this.analyticsQueryService = analyticsQueryService;
        this.applicationCrudService = applicationCrudService;
        this.planCrudService = planCrudService;
        this.instanceQueryService = instanceQueryService;
    }

    public SearchApiAnalyticUseCase.Output execute(ExecutionContext executionContext, SearchApiAnalyticUseCase.Input input) {
        return analyticsQueryService
            .searchApiMetric(executionContext, input.apiId(), input.requestId)
            .map(apiAnalytic -> mapToModel(executionContext, apiAnalytic))
            .orElse(new SearchApiAnalyticUseCase.Output());
    }

    public record Input(String apiId, String requestId) {}

    public record Output(Optional<ApiAnalytic> apiAnalytic) {
        Output(ApiAnalytic apiAnalytic) {
            this(Optional.of(apiAnalytic));
        }
        Output() {
            this(Optional.empty());
        }
    }

    private Output mapToModel(ExecutionContext executionContext, io.gravitee.rest.api.model.v4.analytics.ApiAnalytic apiAnalytic) {
        var result = ApiAnalytic
            .builder()
            .timestamp(apiAnalytic.getTimestamp())
            .apiId(apiAnalytic.getApiId())
            .requestId(apiAnalytic.getRequestId())
            .transactionId(apiAnalytic.getTransactionId())
            .host(apiAnalytic.getHost())
            .application(getApplication(executionContext.getEnvironmentId(), apiAnalytic.getApplicationId()))
            .plan(getPlan(apiAnalytic.getPlanId()))
            .gateway(instanceQueryService.findById(executionContext, apiAnalytic.getGateway()))
            .uri(apiAnalytic.getUri())
            .status(apiAnalytic.getStatus())
            .requestContentLength(apiAnalytic.getRequestContentLength())
            .responseContentLength(apiAnalytic.getResponseContentLength())
            .remoteAddress(apiAnalytic.getRemoteAddress())
            .gatewayLatency(apiAnalytic.getGatewayLatency())
            .gatewayResponseTime(apiAnalytic.getGatewayResponseTime())
            .endpointResponseTime(apiAnalytic.getEndpointResponseTime())
            .method(apiAnalytic.getMethod())
            .endpoint(apiAnalytic.getEndpoint())
            .build();

        return new Output(result);
    }

    private BaseApplicationEntity getApplication(String environmentId, String applicationId) {
        var unknownApp = BaseApplicationEntity.builder().id(applicationId).name(UNKNOWN).build();
        try {
            return applicationId != null ? applicationCrudService.findById(applicationId, environmentId) : unknownApp;
        } catch (ApplicationNotFoundException | TechnicalManagementException e) {
            return unknownApp;
        }
    }

    private GenericPlanEntity getPlan(String planId) {
        final BasePlanEntity unknownPlan = BasePlanEntity.builder().id(planId).name(UNKNOWN).build();
        try {
            return planId != null ? planCrudService.getById(planId) : unknownPlan;
        } catch (PlanNotFoundException | TechnicalManagementException e) {
            return unknownPlan;
        }
    }
}
