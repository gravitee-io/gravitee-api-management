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

import io.gravitee.rest.api.model.v4.analytics.ApiMetricsDetail;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiMetricsDetailAdapter {
    ApiMetricsDetailAdapter INSTANCE = Mappers.getMapper(ApiMetricsDetailAdapter.class);

    ApiMetricsDetail map(io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail apiMetricsDetail);
}
