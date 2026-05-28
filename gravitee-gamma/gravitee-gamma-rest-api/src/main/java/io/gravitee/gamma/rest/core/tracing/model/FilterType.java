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
package io.gravitee.gamma.rest.core.tracing.model;

/**
 * The data shape of a trace filter, used by the UI to pick the right input renderer (single-value
 * chip, multi-select, numeric range, free-text). Matches the {@code apim} management API's
 * {@code FilterSpec.type} vocabulary so a UI built against either API can use the same renderer.
 *
 * @author GraviteeSource Team
 */
public enum FilterType {
    /** Discrete value with no free-text — e.g. service names, ids. UI: chip / autocomplete. */
    KEYWORD,
    /** Free-text — operator typically {@code contains} / {@code starts_with}. UI: text input. */
    STRING,
    /** Numeric — supports range operators ({@code gte} / {@code lte}). UI: number input. */
    NUMBER,
    /** Closed set of values declared by the filter spec's {@code enumValues}. UI: select / multi-select. */
    ENUM,
    /** Two-state — typically {@code eq} only, values {@code "true"} / {@code "false"}. UI: toggle. */
    BOOLEAN,
}
