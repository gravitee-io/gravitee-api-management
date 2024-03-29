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
package io.gravitee.apim.core.api.model.factory;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApi;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.context.IntegrationContext;
import io.gravitee.rest.api.service.common.UuidString;

public class ApiModelFactory {

    private ApiModelFactory() {}

    public static Api fromNewApi(NewApi newApi, String environmentId) {
        var id = UuidString.generateRandom();
        var now = TimeProvider.now();
        return newApi
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .apiDefinitionV4(newApi.toApiDefinitionBuilder().id(id).build())
            .lifecycleState(Api.LifecycleState.STOPPED)
            .build();
    }

    public static Api fromCrd(ApiCRD crd, String environmentId) {
        var id = crd.getId() != null ? crd.getId() : UuidString.generateRandom();
        var now = TimeProvider.now();
        return crd
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .apiDefinitionV4(crd.toApiDefinitionBuilder().id(id).build())
            .build();
    }

    public static Api fromIntegration(IntegrationApi integrationApi, Integration integration) {
        var id = generateFederatedApiId(integrationApi, integration);
        var now = TimeProvider.now();
        return Api
            .builder()
            .id(id)
            .version(integrationApi.version())
            .definitionVersion(DefinitionVersion.FEDERATED)
            .name(integrationApi.name())
            .description(integrationApi.description())
            .createdAt(now)
            .updatedAt(now)
            .environmentId(integration.getEnvironmentId())
            .lifecycleState(null)
            .originContext(new IntegrationContext(integration.getId()))
            .federatedApiDefinition(integrationApi.toFederatedApiDefinitionBuilder().id(id).build())
            .build();
    }

    /**
     * Generate the Federated API identifier.
     *
     * <p>
     *     The id is not randomly generated it is based on
     *     <ul>
     *         <li>environment id</li>
     *         <li>integration id</li>
     *         <li>external API id</li>
     *     </ul>
     * </p>
     * <p>
     *     The combination should produce a unique id that can be recreated to check if a specific API has been already
     *     ingested so we can ignore it.
     * </p>
     * @param integrationApi The external API
     * @param integration The integration
     * @return The generated id
     */
    private static String generateFederatedApiId(IntegrationApi integrationApi, Integration integration) {
        return UuidString.generateForEnvironment(integration.getEnvironmentId(), integration.getId(), integrationApi.id());
    }
}
