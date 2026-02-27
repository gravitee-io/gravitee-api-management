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

import io.gravitee.apim.core.analytics_engine.model.NumberRange;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.LogsFilterSpec;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.LogsFilterSpecRange;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.LogsFilterSpecsResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface LogsDefinitionMapper {
    LogsDefinitionMapper INSTANCE = Mappers.getMapper(LogsDefinitionMapper.class);

    LogsFilterSpec mapFilterSpec(io.gravitee.apim.core.logs_engine.model.LogsFilterSpec filterSpec);

    List<LogsFilterSpec> mapFilterSpecs(List<io.gravitee.apim.core.logs_engine.model.LogsFilterSpec> filterSpecs);

    @Mapping(source = "from", target = "min")
    @Mapping(source = "to", target = "max")
    LogsFilterSpecRange mapRange(NumberRange range);

    default LogsFilterSpecsResponse toFilterSpecsResponse(List<io.gravitee.apim.core.logs_engine.model.LogsFilterSpec> filterSpecs) {
        return new LogsFilterSpecsResponse().data(mapFilterSpecs(filterSpecs));
    }
}
