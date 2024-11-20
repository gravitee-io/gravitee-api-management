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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.InvalidApiLifecycleStateException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.core.workflow.query_service.WorkflowQueryService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@DomainService
public class ApiLifecycleStateDomainService {

    private final WorkflowQueryService workflowQueryService;

    public Api.ApiLifecycleState validateAndSanitizeForUpdate(
        String apiId,
        Api.ApiLifecycleState currentState,
        Api.ApiLifecycleState newState
    ) {
        if (newState == null || (Api.ApiLifecycleState.DEPRECATED != currentState && currentState == newState)) {
            return currentState;
        }

        switch (currentState) {
            case DEPRECATED, ARCHIVED -> throw new InvalidApiLifecycleStateException(newState.name());
            case PUBLISHED, UNPUBLISHED -> {
                if (Api.ApiLifecycleState.CREATED == newState) {
                    throw new InvalidApiLifecycleStateException(newState.name());
                }
            }
            case CREATED -> {
                var status = workflowQueryService.findAllByApiIdAndType(apiId, Workflow.Type.REVIEW);
                if (!status.isEmpty() && Workflow.State.IN_REVIEW == status.get(0).getState()) {
                    throw new InvalidApiLifecycleStateException(newState.name());
                }
            }
        }
        return newState;
    }
}
