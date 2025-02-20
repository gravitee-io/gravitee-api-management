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
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsRequestResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopAppsByRequestCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopHitsApisResponse;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ApiAnalyticsMapper.class })
public interface EnvironmentAnalyticsMapper {
    EnvironmentAnalyticsMapper INSTANCE = Mappers.getMapper(EnvironmentAnalyticsMapper.class);

    EnvironmentAnalyticsResponseStatusRangesResponse map(ResponseStatusRanges responseStatusRanges);

    EnvironmentAnalyticsTopHitsApisResponse map(TopHitsApis topHitsApis);

    EnvironmentAnalyticsRequestResponseTimeResponse map(RequestResponseTime requestResponseTime);

    EnvironmentAnalyticsResponseStatusOvertimeResponse map(ResponseStatusOvertime source);

    EnvironmentAnalyticsTopAppsByRequestCountResponse map(TopHitsApps topHitsApps);
}
