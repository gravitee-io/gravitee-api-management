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
package io.gravitee.apim.usecase.log;

import io.gravitee.apim.storage.analytics.log.LogStorageService;
import io.gravitee.apim.storage.application.ApplicationStorageService;
import io.gravitee.apim.storage.plan.PlanStorageService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.ConnectionLogModel;
import io.gravitee.rest.api.model.v4.log.SearchLogResponse;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SearchConnectionLogUsecase {

    private final LogStorageService logStorageService;
    private final PlanStorageService planStorageService;
    private final ApplicationStorageService applicationStorageService;

    public SearchConnectionLogUsecase(
        LogStorageService logStorageService,
        PlanStorageService planStorageService,
        ApplicationStorageService applicationStorageService
    ) {
        this.logStorageService = logStorageService;
        this.planStorageService = planStorageService;
        this.applicationStorageService = applicationStorageService;
    }

    public Response execute(ExecutionContext executionContext, Request request) {
        var pageable = request.pageable.orElse(new PageableImpl(1, 20));

        var response = logStorageService.searchApiConnectionLog(request.apiId(), pageable);
        return mapToResponse(executionContext, response);
    }

    private Response mapToResponse(ExecutionContext executionContext, SearchLogResponse<BaseConnectionLog> logs) {
        var total = logs.total();
        var data = logs.logs().stream().map(log -> mapToModel(executionContext, log)).toList();

        return new Response(total, data);
    }

    private ConnectionLogModel mapToModel(ExecutionContext executionContext, BaseConnectionLog connectionLog) {
        return ConnectionLogModel
            .builder()
            .apiId(connectionLog.getApiId())
            .requestId(connectionLog.getRequestId())
            .timestamp(connectionLog.getTimestamp())
            .application(getApplicationEntity(executionContext, connectionLog.getApplicationId()))
            .clientIdentifier(connectionLog.getClientIdentifier())
            .method(connectionLog.getMethod())
            .plan(getPlanInfo(connectionLog.getPlanId()))
            .requestEnded(connectionLog.isRequestEnded())
            .transactionId(connectionLog.getTransactionId())
            .status(connectionLog.getStatus())
            .build();
    }

    private GenericPlanEntity getPlanInfo(String planId) {
        try {
            return planStorageService.findById(planId);
        } catch (PlanNotFoundException | TechnicalManagementException e) {
            return BasePlanEntity.builder().id(planId).name("Unknown plan").build();
        }
    }

    private BaseApplicationEntity getApplicationEntity(ExecutionContext executionContext, String applicationId) {
        try {
            return applicationStorageService.findById(executionContext, applicationId);
        } catch (ApplicationNotFoundException | TechnicalManagementException e) {
            return BaseApplicationEntity.builder().id(applicationId).name("Unknown application").build();
        }
    }

    public record Request(String apiId, String userId, Optional<Pageable> pageable) {
        public Request(String apiId, String userId) {
            this(apiId, userId, Optional.empty());
        }
        public Request(String apiId, String userId, Pageable pageable) {
            this(apiId, userId, Optional.of(pageable));
        }
    }

    public record Response(long total, List<ConnectionLogModel> data) {}
}
