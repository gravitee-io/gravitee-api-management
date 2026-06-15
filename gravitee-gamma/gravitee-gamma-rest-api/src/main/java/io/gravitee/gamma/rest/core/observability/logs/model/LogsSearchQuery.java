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
package io.gravitee.gamma.rest.core.observability.logs.model;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * Fully resolved search query passed from the use case to the data port. The {@code apiIds} are
 * already intersected with the caller's accessible scope; the {@code conditions} are pre-validated
 * against the filter registry for the {@code LOGS} signal; the {@code apisById} map provides
 * enrichment context (name + apiType) so the adapter can attach display values without re-loading the
 * API entities.
 *
 * @author GraviteeSource Team
 */
@Builder
public record LogsSearchQuery(
    Set<String> apiIds,
    Map<String, ApiReference> apisById,
    List<FilterCondition> conditions,
    Long from,
    Long to,
    int page,
    int perPage
) {}
