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

import io.gravitee.apim.core.log.use_case.NativeApiLogSummaryUseCase;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLog;
import io.gravitee.rest.api.management.v2.rest.model.NativeApiLogsSummary;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface NativeApiLogsMapper {
    NativeApiLogsMapper INSTANCE = Mappers.getMapper(NativeApiLogsMapper.class);

    default NativeApiLogsSummary mapSummary(NativeApiLogSummaryUseCase.Output output) {
        return new NativeApiLogsSummary().countByConnectionStatus(output.countByConnectionStatus());
    }

    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "mapTimestamp")
    @Mapping(source = "message", target = "errorMessage")
    NativeApiLog map(io.gravitee.apim.core.log.model.NativeApiLog log);

    List<NativeApiLog> mapList(List<io.gravitee.apim.core.log.model.NativeApiLog> logs);
}
