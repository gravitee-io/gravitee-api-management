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
import io.gravitee.apim.core.analytics.model.ApiMetricsDetail;
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
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class FindApiMetricsDetailUseCase {

    static final String UNKNOWN = "Unknown";
    private final AnalyticsQueryService analyticsQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final PlanCrudService planCrudService;

    public FindApiMetricsDetailUseCase.Output execute(ExecutionContext executionContext, FindApiMetricsDetailUseCase.Input input) {
        return analyticsQueryService
            .findApiMetricsDetail(executionContext, input.apiId(), input.requestId)
            .map(apiMetricsDetail -> mapToModel(executionContext, apiMetricsDetail))
            .orElse(new FindApiMetricsDetailUseCase.Output());
    }

    public record Input(String apiId, String requestId) {}

    public record Output(Optional<ApiMetricsDetail> apiMetricsDetail) {
        Output(ApiMetricsDetail apiMetricsDetail) {
            this(Optional.of(apiMetricsDetail));
        }

        Output() {
            this(Optional.empty());
        }
    }

    private Output mapToModel(
        ExecutionContext executionContext,
        io.gravitee.rest.api.model.v4.analytics.ApiMetricsDetail apiMetricsDetail
    ) {
        var result = ApiMetricsDetail.builder()
            .timestamp(apiMetricsDetail.getTimestamp())
            .apiId(apiMetricsDetail.getApiId())
            .requestId(apiMetricsDetail.getRequestId())
            .transactionId(apiMetricsDetail.getTransactionId())
            .host(apiMetricsDetail.getHost())
            .application(getApplication(executionContext.getEnvironmentId(), apiMetricsDetail.getApplicationId()))
            .plan(getPlan(apiMetricsDetail.getPlanId()))
            .gateway(apiMetricsDetail.getGateway())
            .uri(apiMetricsDetail.getUri())
            .status(apiMetricsDetail.getStatus())
            .requestContentLength(apiMetricsDetail.getRequestContentLength())
            .responseContentLength(apiMetricsDetail.getResponseContentLength())
            .remoteAddress(apiMetricsDetail.getRemoteAddress())
            .gatewayLatency(apiMetricsDetail.getGatewayLatency())
            .gatewayResponseTime(apiMetricsDetail.getGatewayResponseTime())
            .endpointResponseTime(apiMetricsDetail.getEndpointResponseTime())
            .method(apiMetricsDetail.getMethod())
            .endpoint(apiMetricsDetail.getEndpoint())
            .message(apiMetricsDetail.getMessage())
            .errorKey(apiMetricsDetail.getErrorKey())
            .errorComponentName(apiMetricsDetail.getErrorComponentName())
            .errorComponentType(apiMetricsDetail.getErrorComponentType())
            .warnings(apiMetricsDetail.getWarnings())
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
