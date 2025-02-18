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
package io.gravitee.apim.core.log.use_case;

import static io.gravitee.apim.core.log.domain_service.ConnectionLogMetadataDomainService.UNKNOWN_SERVICE;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.log.domain_service.ConnectionLogMetadataDomainService;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchApplicationConnectionLogsUseCase {

    private static final String UNKNOWN = "Unknown";
    private final ConnectionLogsCrudService connectionLogsCrudService;
    private final ApplicationCrudService applicationCrudService;
    private final PlanCrudService planCrudService;
    private final ApiCrudService apiCrudService;
    private final ConnectionLogMetadataDomainService connectionLogMetadataDomainService;

    public Output execute(ExecutionContext executionContext, Input input) {
        // Will throw exception if application does not exist for the given environment
        applicationCrudService.findById(input.applicationId(), executionContext.getEnvironmentId());

        var pageable = input.pageable.orElse(new PageableImpl(1, 20));

        var response = connectionLogsCrudService.searchApplicationConnectionLogs(
            executionContext,
            input.applicationId(),
            input.logsFilters(),
            pageable
        );
        return mapToResponse(response);
    }

    private Output mapToResponse(SearchLogsResponse<BaseConnectionLog> logs) {
        var total = logs.total();
        var data = logs.logs().stream().map(this::mapToModel).toList();
        var metadata = connectionLogMetadataDomainService.getMetadataForApplicationConnectionLog(data);

        return new Output(total, data, metadata);
    }

    private ConnectionLog mapToModel(BaseConnectionLog connectionLog) {
        return ConnectionLog
            .builder()
            .applicationId(connectionLog.getApplicationId())
            .apiId(connectionLog.getApiId())
            .api(getApiInfo(connectionLog.getApiId()))
            .requestId(connectionLog.getRequestId())
            .timestamp(connectionLog.getTimestamp())
            .clientIdentifier(connectionLog.getClientIdentifier() != null ? connectionLog.getClientIdentifier() : UNKNOWN)
            .method(connectionLog.getMethod())
            .planId(connectionLog.getPlanId())
            .plan(getPlanInfo(connectionLog.getPlanId()))
            .requestEnded(connectionLog.isRequestEnded())
            .transactionId(connectionLog.getTransactionId())
            .status(connectionLog.getStatus())
            .uri(connectionLog.getUri())
            .gateway(connectionLog.getGateway())
            .gatewayResponseTime(connectionLog.getGatewayResponseTime())
            .build();
    }

    private Api getApiInfo(String apiId) {
        var unknownApi = Api.builder().id(UNKNOWN_SERVICE).build();
        if (apiId == null) {
            return unknownApi;
        }

        try {
            return apiCrudService.get(apiId);
        } catch (ApiNotFoundException | TechnicalManagementException e) {
            return unknownApi;
        }
    }

    private Plan getPlanInfo(String planId) {
        var unknownPlan = Plan.builder().id(UNKNOWN_SERVICE).build();
        if (planId == null) {
            return unknownPlan;
        }
        try {
            return planCrudService.getById(planId);
        } catch (PlanNotFoundException | TechnicalManagementException e) {
            return unknownPlan;
        }
    }

    public record Input(String applicationId, SearchLogsFilters logsFilters, Optional<Pageable> pageable) {
        public Input(String applicationId, SearchLogsFilters logsFilters) {
            this(applicationId, logsFilters, Optional.empty());
        }
        public Input(String applicationId, SearchLogsFilters logsFilters, Pageable pageable) {
            this(applicationId, logsFilters, Optional.of(pageable));
        }
    }

    public record Output(long total, List<ConnectionLog> data, Map<String, Map<String, String>> metadata) {}
}
