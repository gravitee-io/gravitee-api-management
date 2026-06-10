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

import java.util.List;

/**
 * One filter the caller wants applied to an observability query — references a {@link FilterSpec}
 * by {@link #name} and pairs it with an {@link #operator} + values to match.
 *
 * <p>{@link #values} is always a list — single-value operators like {@code EQ} pass a one-element
 * list, while {@code IN} / {@code NOT_IN} carry multiple entries natively.
 *
 * @author GraviteeSource Team
 */
public record FilterCondition(String name, FilterOperator operator, List<String> values) {}
