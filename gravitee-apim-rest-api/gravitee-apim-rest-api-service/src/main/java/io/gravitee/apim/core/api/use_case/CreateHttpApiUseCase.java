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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.NewHttpApi;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;

@UseCase
public class CreateHttpApiUseCase {

    private static final List<ApiType> SUPPORTED_API_TYPES = List.of(
        ApiType.PROXY,
        ApiType.MESSAGE,
        ApiType.MCP_PROXY,
        ApiType.LLM_PROXY,
        ApiType.A2A_PROXY
    );

    private final ValidateApiDomainService validateApiDomainService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final CreateApiDomainService createApiDomainService;

    public CreateHttpApiUseCase(
        ValidateApiDomainService validateApiDomainService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        CreateApiDomainService createApiDomainService
    ) {
        this.validateApiDomainService = validateApiDomainService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.createApiDomainService = createApiDomainService;
    }

    public record Input(NewHttpApi newHttpApi, AuditInfo auditInfo) {
        public Input {
            if (newHttpApi == null || !SUPPORTED_API_TYPES.contains(newHttpApi.getType())) {
                throw new ApiInvalidTypeException(SUPPORTED_API_TYPES);
            }
        }
    }

    public record Output(ApiWithFlows api) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo;

        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId()
        );

        Api newApi = ApiModelFactory.fromNewHttpApi(input.newHttpApi, auditInfo.environmentId());
        if (newApi.getType() == ApiType.LLM_PROXY && newApi.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api v4Api) {
            v4Api.setFlows(defaultFlowsLlmProxy());
        }

        var created = createApiDomainService.create(
            newApi,
            primaryOwner,
            auditInfo,
            api ->
                validateApiDomainService.validateAndSanitizeForCreation(
                    api,
                    primaryOwner,
                    auditInfo.environmentId(),
                    auditInfo.organizationId()
                ),
            oneShotIndexation(auditInfo)
        );

        return new Output(created);
    }

    private static @NonNull List<Flow> defaultFlowsLlmProxy() {
        return List.of(
            new Flow().withSelectors(
                List.of(
                    HttpSelector.builder().pathOperator(Operator.EQUALS).path("/chat/completions").methods(Set.of(HttpMethod.POST)).build()
                )
            ),
            new Flow().withSelectors(
                List.of(HttpSelector.builder().pathOperator(Operator.EQUALS).path("/models").methods(Set.of(HttpMethod.GET)).build())
            ),
            new Flow().withSelectors(
                List.of(HttpSelector.builder().pathOperator(Operator.EQUALS).path("/embeddings").methods(Set.of(HttpMethod.POST)).build())
            )
        );
    }
}
