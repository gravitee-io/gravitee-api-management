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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogModel;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;

@UseCase
public class SearchConnectionLogsUseCase {

    static final String UNKNOWN = "Unknown";
    private final ConnectionLogsCrudService connectionLogsCrudService;
    private final PlanCrudService planCrudService;
    private final ApplicationCrudService applicationCrudService;

    public SearchConnectionLogsUseCase(
        ConnectionLogsCrudService connectionLogsCrudService,
        PlanCrudService planCrudService,
        ApplicationCrudService applicationCrudService
    ) {
        this.connectionLogsCrudService = connectionLogsCrudService;
        this.planCrudService = planCrudService;
        this.applicationCrudService = applicationCrudService;
    }

    public Output execute(ExecutionContext executionContext, Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 20));

        var response = connectionLogsCrudService.searchApiConnectionLogs(executionContext, input.apiId(), input.logsFilters(), pageable);
        return mapToResponse(executionContext, response);
    }

    private Output mapToResponse(ExecutionContext executionContext, SearchLogsResponse<BaseConnectionLog> logs) {
        var total = logs.total();
        var data = logs.logs().stream().map(log -> mapToModel(executionContext, log)).toList();

        return new Output(total, data);
    }

    private ConnectionLogModel mapToModel(ExecutionContext executionContext, BaseConnectionLog connectionLog) {
        return ConnectionLogModel
            .builder()
            .apiId(connectionLog.getApiId())
            .requestId(connectionLog.getRequestId())
            .timestamp(connectionLog.getTimestamp())
            .application(getApplicationEntity(executionContext, connectionLog.getApplicationId()))
            .clientIdentifier(connectionLog.getClientIdentifier() != null ? connectionLog.getClientIdentifier() : UNKNOWN)
            .method(connectionLog.getMethod())
            .plan(getPlanInfo(connectionLog.getPlanId()))
            .requestEnded(connectionLog.isRequestEnded())
            .transactionId(connectionLog.getTransactionId())
            .status(connectionLog.getStatus())
            .build();
    }

    private GenericPlanEntity getPlanInfo(String planId) {
        final BasePlanEntity unknownPlan = BasePlanEntity.builder().id(planId).name(UNKNOWN).build();
        try {
            return planId != null ? planCrudService.getById(planId) : unknownPlan;
        } catch (PlanNotFoundException | TechnicalManagementException e) {
            return unknownPlan;
        }
    }

    private BaseApplicationEntity getApplicationEntity(ExecutionContext executionContext, String applicationId) {
        try {
            return applicationCrudService.findById(executionContext, applicationId);
        } catch (ApplicationNotFoundException | TechnicalManagementException e) {
            return BaseApplicationEntity.builder().id(applicationId).name(UNKNOWN).build();
        }
    }

    public record Input(String apiId, SearchLogsFilters logsFilters, Optional<Pageable> pageable) {
        public Input(String apiId, SearchLogsFilters logsFilters) {
            this(apiId, logsFilters, Optional.empty());
        }
        public Input(String apiId, SearchLogsFilters logsFilters, Pageable pageable) {
            this(apiId, logsFilters, Optional.of(pageable));
        }
    }

    public record Output(long total, List<ConnectionLogModel> data) {}
}
