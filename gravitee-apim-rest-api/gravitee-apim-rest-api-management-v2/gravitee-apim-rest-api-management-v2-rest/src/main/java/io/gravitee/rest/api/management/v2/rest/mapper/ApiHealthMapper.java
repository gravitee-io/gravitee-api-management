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

import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiHealthAverageResponseTimeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface ApiHealthMapper {
    Logger logger = LoggerFactory.getLogger(ApiHealthMapper.class);
    ApiHealthMapper INSTANCE = Mappers.getMapper(ApiHealthMapper.class);

    @Mapping(source = "globalResponseTimeMs", target = "global")
    @Mapping(source = "groupedResponseTimeMs", target = "group")
    ApiHealthAverageResponseTimeResponse map(AverageHealthCheckResponseTime source);

    @Mapping(target = "data", source = "buckets")
    ApiHealthAverageResponseTimeOvertimeResponse map(AverageHealthCheckResponseTimeOvertime source);

    @Mapping(target = "from", expression = "java(source.from().toEpochMilli())")
    @Mapping(target = "to", expression = "java(source.to().toEpochMilli())")
    @Mapping(target = "interval", expression = "java(source.interval().toMillis())")
    AnalyticTimeRange map(AverageHealthCheckResponseTimeOvertime.TimeRange source);
}
