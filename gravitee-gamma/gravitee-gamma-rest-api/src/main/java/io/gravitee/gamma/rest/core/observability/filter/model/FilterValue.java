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
package io.gravitee.gamma.rest.core.observability.filter.model;

/**
 * One selectable value of a filter, returned by the filter-values endpoint.
 *
 * @param value The stable value sent back on the wire as a {@link FilterCondition} value. For
 *              id-based KEYWORD filters (e.g. {@code API}) this is the entity id; for ENUM filters it
 *              is the enum constant.
 * @param label Human-readable display label ({@code null} when value and label are identical).
 *
 * @author GraviteeSource Team
 */
public record FilterValue(String value, String label) {}
