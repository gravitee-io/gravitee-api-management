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
package io.gravitee.apim.core.async_api;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.async_api.exception.AsyncApiContentEmptyException;

/**
 * Domain service for validating AsyncAPI specification content.
 *
 * <p>Provides shared validation logic that can be reused across multiple domains
 * (portal pages, documentation, etc.).</p>
 *
 * @author Gravitee.io Team
 */
@DomainService
public class AsyncApiValidator {

    /**
     * Validates that AsyncAPI content is not null or empty.
     *
     * @param content the AsyncAPI content to validate
     * @throws AsyncApiContentEmptyException if content is null, empty, or whitespace-only
     */
    public void validateNotEmpty(AsyncApi content) {
        if (content.value() == null || content.value().trim().isEmpty()) {
            throw new AsyncApiContentEmptyException();
        }
    }
}
