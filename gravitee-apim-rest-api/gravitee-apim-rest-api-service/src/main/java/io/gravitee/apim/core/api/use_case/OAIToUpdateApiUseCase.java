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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.exception.InvalidApiDefinitionException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class OAIToUpdateApiUseCase {

    @Builder
    public record Input(
        String apiId,
        ImportSwaggerDescriptorEntity importSwaggerDescriptor,
        boolean withDocumentation,
        boolean withOASValidationPolicy,
        boolean withPolicyPaths,
        AuditInfo auditInfo
    ) {}

    public record Output(ApiWithFlows apiWithFlows) {}

    private final OAIDomainService oaiDomainService;
    private final FlowCrudService flowCrudService;
    private final UpdateApiDefinitionUseCase updateApiDefinitionUseCase;

    public Output execute(Input input) {
        var audit = input.auditInfo();
        var importDefinition = oaiDomainService.convert(
            audit.organizationId(),
            audit.environmentId(),
            input.importSwaggerDescriptor(),
            input.withDocumentation(),
            input.withOASValidationPolicy()
        );

        if (importDefinition == null) {
            throw new InvalidApiDefinitionException("Unable to read the swagger specification");
        }

        if (!input.withPolicyPaths()) {
            restoreApiFlowIdsByKey(
                importDefinition.getApiExport() != null ? importDefinition.getApiExport().getFlows() : null,
                input.apiId()
            );
        }

        var output = updateApiDefinitionUseCase.execute(new UpdateApiDefinitionUseCase.Input(input.apiId(), importDefinition, audit));

        return new Output(output.apiWithFlows());
    }

    private void restoreApiFlowIdsByKey(List<? extends AbstractFlow> flows, String apiId) {
        if (flows == null || flows.stream().noneMatch(f -> f instanceof Flow flow && flow.getId() == null)) {
            return;
        }

        Function<Flow, String> httpFlowKey = flow ->
            flow.getSelectors() == null
                ? ""
                : flow
                    .getSelectors()
                    .stream()
                    .filter(HttpSelector.class::isInstance)
                    .map(HttpSelector.class::cast)
                    .map(http -> {
                        var methods = http.getMethods() != null
                            ? http.getMethods().stream().map(Enum::name).sorted().collect(Collectors.joining(","))
                            : "";
                        return http.getPath() + "|" + methods;
                    })
                    .collect(Collectors.joining(";"));

        var idByKey = flowCrudService
            .getApiV4Flows(apiId)
            .stream()
            .filter(f -> f instanceof Flow flow && flow.getId() != null)
            .collect(
                Collectors.toMap(httpFlowKey, Flow::getId, (firstId, duplicateId) -> {
                    log.warn(
                        "Duplicate HTTP flow key found among persisted flows for API [{}], keeping first flow id [{}]",
                        apiId,
                        firstId
                    );
                    return firstId;
                })
            );

        flows
            .stream()
            .filter(f -> f instanceof Flow flow && flow.getId() == null)
            .map(Flow.class::cast)
            .forEach(flow -> {
                // remove so the same existing ID cannot be assigned to more than one incoming flow
                var existingId = idByKey.remove(httpFlowKey.apply(flow));
                if (existingId != null) {
                    flow.setId(existingId);
                }
            });
    }
}
