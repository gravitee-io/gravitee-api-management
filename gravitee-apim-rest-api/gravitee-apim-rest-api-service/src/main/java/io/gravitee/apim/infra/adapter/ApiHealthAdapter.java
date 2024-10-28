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

import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface ApiHealthAdapter {
    Logger LOGGER = LoggerFactory.getLogger(ApiHealthAdapter.class);
    ApiHealthAdapter INSTANCE = Mappers.getMapper(ApiHealthAdapter.class);

    io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeQuery map(
        ApiHealthQueryService.AverageHealthCheckResponseTimeQuery source
    );

    AverageHealthCheckResponseTime map(io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTime source);
}
