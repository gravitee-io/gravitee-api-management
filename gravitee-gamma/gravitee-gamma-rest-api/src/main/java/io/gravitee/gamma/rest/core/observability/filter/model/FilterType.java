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
 * The data shape of an observability filter, used by the UI to pick the right input renderer
 * (single-value chip, multi-select, numeric range, free-text). Mirrors the tracing-side
 * {@code FilterType} vocabulary so both APIs can share the same UI renderer logic.
 *
 * @author GraviteeSource Team
 */
public enum FilterType {
    KEYWORD,
    STRING,
    NUMBER,
    ENUM,
    BOOLEAN,
}
