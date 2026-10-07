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
package io.gravitee.apim.core.ai_workspace.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceMembershipQuery;
import io.gravitee.apim.core.ai_workspace.domain_service.AiWorkspaceModelsReader;
import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceModel;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@UseCase
public class ListMyAiWorkspaceModelsUseCase {

    private final AiWorkspaceMembershipQuery memberships;
    private final ApiProductQueryService apiProductQueryService;
    private final ApiCrudService apiCrudService;

    public ListMyAiWorkspaceModelsUseCase(
        AiWorkspaceMembershipQuery memberships,
        ApiProductQueryService apiProductQueryService,
        ApiCrudService apiCrudService
    ) {
        this.memberships = memberships;
        this.apiProductQueryService = apiProductQueryService;
        this.apiCrudService = apiCrudService;
    }

    public record Input(ExecutionContext executionContext, Set<String> applicationIds, String aiWorkspaceId) {}

    public record Output(List<AiWorkspaceModel> models) {}

    public Output execute(Input input) {
        String id = input.aiWorkspaceId();
        if (id == null || id.isBlank()) {
            throw new AiWorkspaceNotFoundException(id);
        }
        AiWorkspaceMembership membership = memberships
            .find(input.executionContext(), input.applicationIds(), Set.of(id))
            .stream()
            .filter(item -> id.equals(item.apiProductId()))
            .findFirst()
            .orElseThrow(() -> new AiWorkspaceNotFoundException(id));
        // membership is required for access; product supplies the proxy API ids
        ApiProduct product = apiProductQueryService
            .findByEnvironmentIdAndIdIn(input.executionContext().getEnvironmentId(), Set.of(membership.apiProductId()))
            .stream()
            .filter(item -> item.getKind() == ApiProductKind.AI_WORKSPACE)
            .findFirst()
            .orElseThrow(() -> new AiWorkspaceNotFoundException(id));
        List<Api> apis = product.getApiIds() == null
            ? List.of()
            : product.getApiIds().stream().map(apiCrudService::findById).flatMap(Optional::stream).toList();
        return new Output(AiWorkspaceModelsReader.read(input.executionContext().getEnvironmentId(), apis));
    }
}
