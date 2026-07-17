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
package io.gravitee.gamma.rest.resources.tracing.dto;

import io.gravitee.gamma.rest.resources.tracing.dto.SearchTracesRequestDto.TimeRangeDto;
import java.util.List;

/**
 * Request body for {@code POST /attributes/{name}/values} — the grouped attribute-values view (e.g. the Agent
 * Control Tower "Conversations" list, grouping by {@code gravitee.conversation.id}).
 *
 * <p>{@code apiId} is required and scopes the query to a single API (same rationale as {@code SearchTracesRequestDto}).
 * {@code timeRange} is the ISO window; {@code null} bounds defer to the use case's "last 24h" default. {@code correlate}
 * lists additional span-attribute keys to return per value — for each, the top value within the group is returned (e.g.
 * {@code gravitee.entrypoint.id} to surface which entrypoint served a conversation). Both the grouped attribute and the
 * correlated ones live on the same (root) span, so they aggregate together in a single query.
 */
public record AttributeValuesRequestDto(String apiId, TimeRangeDto timeRange, List<String> correlate) {}
