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
package io.gravitee.gamma.rest.core.guardrail.exception;

import io.gravitee.apim.core.exception.ConflictDomainException;

/**
 * The internal flow of the API changed between the moment it was read and the moment it was about to be written.
 * Writing anyway would drop whatever the other call put there, so the caller reads the API again and decides.
 */
public class ApiInternalFlowChangedException extends ConflictDomainException {

    public ApiInternalFlowChangedException(String apiId) {
        super("API '" + apiId + "' changed while its Guardrails were being set: read it again and set them from there.");
    }
}
