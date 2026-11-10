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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.ApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductFlows;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductInfo;
import io.gravitee.rest.api.management.v2.rest.model.FlowExecution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface ApiProductMapper {
    ApiProductMapper INSTANCE = Mappers.getMapper(ApiProductMapper.class);

    ApiProduct map(io.gravitee.apim.core.api_product.model.ApiProduct coreApiProduct);

    ApiProductInfo map(io.gravitee.apim.core.api_product.model.ApiProductInfo coreApiProductInfo);

    default ApiProductFlows map(io.gravitee.apim.core.api_product.model.ApiProductFlows coreApiProductFlows) {
        return new ApiProductFlows()
            .flowExecution(map(coreApiProductFlows.flowExecution()))
            .flows(FlowMapper.INSTANCE.mapFromHttpV4(coreApiProductFlows.flows()));
    }

    default io.gravitee.apim.core.api_product.model.ApiProductFlows map(ApiProductFlows apiProductFlows) {
        return new io.gravitee.apim.core.api_product.model.ApiProductFlows(
            map(apiProductFlows.getFlowExecution()),
            FlowMapper.INSTANCE.mapToHttpV4(apiProductFlows.getFlows())
        );
    }

    FlowExecution map(io.gravitee.definition.model.v4.flow.execution.FlowExecution coreFlowExecution);

    @Mapping(target = "mode", defaultValue = "DEFAULT")
    io.gravitee.definition.model.v4.flow.execution.FlowExecution map(FlowExecution flowExecution);
}
