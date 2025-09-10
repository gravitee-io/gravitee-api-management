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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.integration.api.model.PlanSecurityType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface IntegrationAdapter {
    IntegrationAdapter INSTANCE = Mappers.getMapper(IntegrationAdapter.class);
    SpecificApiAdapter SPECIFIC_API_INTEGRATION_ADAPTER = Mappers.getMapper(SpecificApiAdapter.class);
    SpecificA2aAdapter SPECIFIC_A2A_INTEGRATION_ADAPTER = Mappers.getMapper(SpecificA2aAdapter.class);

    default Integration toEntity(io.gravitee.repository.management.model.Integration integration) {
        return integration.isA2aIntegration()
            ? SPECIFIC_A2A_INTEGRATION_ADAPTER.toEntity(integration)
            : SPECIFIC_API_INTEGRATION_ADAPTER.toEntity(integration);
    }

    default io.gravitee.repository.management.model.Integration toRepository(Integration integration) {
        return switch (integration) {
            case Integration.ApiIntegration api -> SPECIFIC_API_INTEGRATION_ADAPTER.toRepository(api);
            case Integration.A2aIntegration a2a -> SPECIFIC_A2A_INTEGRATION_ADAPTER.toRepository(a2a);
        };
    }

    IntegrationApi map(io.gravitee.integration.api.model.Api source, String integrationId);

    @Mapping(source = "planSecurityType", target = "type")
    IntegrationApi.Plan map(io.gravitee.integration.api.model.Plan source);

    @ValueMapping(source = "API_KEY", target = "API_KEY")
    @ValueMapping(source = "JWT", target = "JWT")
    @ValueMapping(source = "OAUTH2", target = "OAUTH2")
    IntegrationApi.PlanType map(PlanSecurityType source);

    IntegrationApi.Page map(io.gravitee.integration.api.model.Page source);

    @SuppressWarnings("unchecked") //the type is safe, but it’s mandatory due to type erasure
    default <T extends Integration> SpecificAdapter<T> specific(T integration) {
        return (SpecificAdapter<T>) switch (integration) {
            case Integration.ApiIntegration ignored -> SPECIFIC_API_INTEGRATION_ADAPTER;
            case Integration.A2aIntegration ignored -> SPECIFIC_A2A_INTEGRATION_ADAPTER;
        };
    }

    interface SpecificAdapter<T extends Integration> {
        T toEntity(io.gravitee.repository.management.model.Integration integration);
        io.gravitee.repository.management.model.Integration toRepository(T integration);
    }

    @Mapper
    interface SpecificApiAdapter extends SpecificAdapter<Integration.ApiIntegration> {
        Integration.ApiIntegration toEntity(io.gravitee.repository.management.model.Integration integration);

        io.gravitee.repository.management.model.Integration toRepository(Integration.ApiIntegration integration);
    }

    @Mapper
    interface SpecificA2aAdapter extends SpecificAdapter<Integration.A2aIntegration> {
        Integration.A2aIntegration toEntity(io.gravitee.repository.management.model.Integration integration);

        @Mapping(target = "provider", constant = "A2A")
        io.gravitee.repository.management.model.Integration toRepository(Integration.A2aIntegration integration);
    }
}
