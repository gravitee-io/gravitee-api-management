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
package io.gravitee.apim.core.gravitee_markdown;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import lombok.RequiredArgsConstructor;

/**
 * Domain service for validating Gravitee Markdown (GMD) content.
 *
 * <p>This validator operates on the {@link GraviteeMarkdownContainer} interface,
 * making it agnostic to specific domain implementations. It provides shared validation
 * logic that can be reused across multiple domains (portal pages, subscription forms, etc.).</p>
 *
 * @author Gravitee.io Team
 */
@DomainService
@RequiredArgsConstructor
public class GraviteeMarkdownValidator {

    /**
     * Validates that GMD content is not null or empty.
     *
     * <p>Accepts {@link GraviteeMarkdownContainer} to establish the validation contract.
     * Callers can pass domain entities that implement the interface, or use lambda syntax
     * for functional-style validation: {@code validateNotEmpty(() -> content)}.</p>
     *
     * @param container the container holding GMD content
     * @throws GraviteeMarkdownContentEmptyException if content is null, empty, or whitespace-only
     */
    public void validateNotEmpty(GraviteeMarkdownContainer container) {
        String content = container.getGmdContent();
        if (content == null || content.trim().isEmpty()) {
            throw new GraviteeMarkdownContentEmptyException();
        }
    }
}
