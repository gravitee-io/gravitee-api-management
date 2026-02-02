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

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LogsEngineMapper {
    LogsEngineMapper INSTANCE = Mappers.getMapper(LogsEngineMapper.class);

    @Mapping(target = "page", source = "page")
    @Mapping(target = "perPage", source = "perPage")
    SearchLogsRequest fromRequestEntity(
        io.gravitee.rest.api.management.v2.rest.model.logs.engine.SearchLogsRequest requestEntity,
        Integer page,
        Integer perPage
    );

    SearchLogsResponse fromResponseModel(io.gravitee.apim.core.logs_engine.model.SearchLogsResponse responseModel);

    default Filter fromFilterEntity(io.gravitee.rest.api.management.v2.rest.model.logs.engine.Filter filterEntity) {
        if (filterEntity == null) {
            return null;
        }
        var instance = filterEntity.getActualInstance();
        if (instance == null) {
            return null;
        }
        return switch (instance) {
            case io.gravitee.rest.api.management.v2.rest.model.logs.engine.StringFilter s -> new Filter(
                new StringFilter(mapFilterName(s.getName()), mapOperator(s.getOperator()), s.getValue())
            );
            case io.gravitee.rest.api.management.v2.rest.model.logs.engine.ArrayFilter a -> new Filter(
                new ArrayFilter(mapFilterName(a.getName()), mapOperator(a.getOperator()), a.getValue())
            );
            default -> throw new ValidationDomainException("unknown filter type");
        };
    }

    default FilterName mapFilterName(io.gravitee.rest.api.management.v2.rest.model.logs.engine.FilterName filterName) {
        return FilterName.valueOf(filterName.name());
    }

    default Operator mapOperator(io.gravitee.rest.api.management.v2.rest.model.logs.engine.Operator operator) {
        return Operator.valueOf(operator.name());
    }
}
