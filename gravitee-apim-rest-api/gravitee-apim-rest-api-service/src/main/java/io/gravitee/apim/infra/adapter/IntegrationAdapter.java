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

    Integration toEntity(io.gravitee.repository.management.model.Integration integration);

    io.gravitee.repository.management.model.Integration toRepository(Integration integration);

    IntegrationApi map(io.gravitee.integration.api.model.Api source, String integrationId);

    @Mapping(source = "planSecurityType", target = "type")
    IntegrationApi.Plan map(io.gravitee.integration.api.model.Plan source);

    @ValueMapping(source = "API_KEY", target = "API_KEY")
    @ValueMapping(source = "JWT", target = MappingConstants.NULL)
    @ValueMapping(source = "OAUTH2", target = "OAUTH2")
    IntegrationApi.PlanType map(PlanSecurityType source);

    IntegrationApi.Page map(io.gravitee.integration.api.model.Page source);
}
