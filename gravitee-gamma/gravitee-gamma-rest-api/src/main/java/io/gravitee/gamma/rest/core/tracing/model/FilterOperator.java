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
 * Comparison operator a filter supports. Mirrors the {@code FILTER_OPERATORS} union from
 * {@code @gravitee/gamma-lib-observability}'s {@code domain.ts} 1:1 (same set, same lower-case
 * spelling on the wire) so a {@code FilterCondition} built by the lib's UI flows straight to this
 * backend without translation.
 *
 * @author GraviteeSource Team
 */
public enum FilterOperator {
    EQ,
    NEQ,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GT,
    GTE,
    LT,
    LTE,
    IN,
    NOT_IN,
    EXISTS,
    NOT_EXISTS,
}
