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
package io.gravitee.apim.core.mcp_tool.model;

import lombok.Builder;

/**
 * A non-fatal error or warning collected while parsing the OpenAPI specification.
 * The {@code key} mirrors the values used by the front-end parser
 * ({@code invalidFormat}, {@code invalidSpec}, {@code invalidRefs}, {@code duplicateName})
 * so both producers expose the same vocabulary.
 */
@Builder(toBuilder = true)
public record ParseError(String key, String message) {
    public static final String INVALID_FORMAT = "invalidFormat";
    public static final String INVALID_SPEC = "invalidSpec";
    public static final String INVALID_REFS = "invalidRefs";
    public static final String DUPLICATE_NAME = "duplicateName";
}
