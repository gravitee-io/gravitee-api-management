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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceBudget;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceConsumption;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceDetails;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceKey;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceModelInfo;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspacePage;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceSummary;
import io.gravitee.rest.api.portal.rest.model.AiWorkspace;
import io.gravitee.rest.api.portal.rest.model.AiWorkspaceModel;
import io.gravitee.rest.api.portal.rest.model.AiWorkspacesResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class AiWorkspaceMapper {

    private AiWorkspaceMapper() {}

    public static AiWorkspacesResponse toResponse(AiWorkspacePage page) {
        AiWorkspacesResponse response = new AiWorkspacesResponse();
        response.setData(page.data() == null ? List.of() : page.data().stream().map(AiWorkspaceMapper::toSummary).toList());
        response.setPage(page.page());
        response.setSize(page.size());
        response.setTotal((long) page.total());
        return response;
    }

    public static io.gravitee.rest.api.portal.rest.model.AiWorkspaceSummary toSummary(AiWorkspaceSummary summary) {
        var target = new io.gravitee.rest.api.portal.rest.model.AiWorkspaceSummary();
        target.setId(summary.id());
        target.setName(summary.name());
        target.setDescription(summary.description());
        target.setBudget(toBudget(summary.budget()));
        return target;
    }

    public static AiWorkspace toDetails(AiWorkspaceDetails details) {
        AiWorkspace target = new AiWorkspace();
        target.setId(details.id());
        target.setName(details.name());
        target.setDescription(details.description());
        target.setBudget(toBudget(details.budget()));
        target.setEndpointUrl(details.endpointUrl());
        target.setKey(toKey(details.key()));
        target.setModels(details.models() == null ? List.of() : details.models().stream().map(AiWorkspaceMapper::toModel).toList());
        return target;
    }

    public static io.gravitee.rest.api.portal.rest.model.AiWorkspaceConsumption toConsumption(AiWorkspaceConsumption consumption) {
        var target = new io.gravitee.rest.api.portal.rest.model.AiWorkspaceConsumption();
        target.setTokens(consumption.tokens());
        target.setRequests(consumption.requests());
        target.setCost(consumption.cost());
        target.setFrom(OffsetDateTime.ofInstant(consumption.from(), ZoneOffset.UTC));
        target.setTo(OffsetDateTime.ofInstant(consumption.to(), ZoneOffset.UTC));
        return target;
    }

    private static io.gravitee.rest.api.portal.rest.model.AiWorkspaceBudget toBudget(AiWorkspaceBudget budget) {
        if (budget == null) {
            return null;
        }
        var target = new io.gravitee.rest.api.portal.rest.model.AiWorkspaceBudget();
        target.setAmount(budget.amount() == null ? null : budget.amount().doubleValue());
        target.setPeriod(budget.period());
        return target;
    }

    private static io.gravitee.rest.api.portal.rest.model.AiWorkspaceKey toKey(AiWorkspaceKey key) {
        if (key == null) {
            return null;
        }
        var target = new io.gravitee.rest.api.portal.rest.model.AiWorkspaceKey();
        target.setValue(key.value());
        target.setStatus(key.status());
        target.setCreatedAt(key.createdAt() == null ? null : OffsetDateTime.ofInstant(key.createdAt(), ZoneOffset.UTC));
        return target;
    }

    private static AiWorkspaceModel toModel(AiWorkspaceModelInfo model) {
        AiWorkspaceModel target = new AiWorkspaceModel();
        target.setName(model.name());
        target.setInputPrice(model.inputPrice());
        target.setOutputPrice(model.outputPrice());
        return target;
    }
}
