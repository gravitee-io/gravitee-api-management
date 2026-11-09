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
package io.gravitee.gamma.rest.core.guardian.exception;

import io.gravitee.apim.core.exception.ConflictDomainException;

/**
 * Thrown when a Guardian is created, or renamed, with a name another Guardian of the same environment
 * already carries. A 409: the request is well formed and the caller may retry it with another name.
 *
 * <p>Case is ignored, as it is for the check itself — "Credential leakage" and "credential leakage"
 * are the same name to someone reading a list.
 *
 * @author GraviteeSource Team
 */
public class GuardianNameAlreadyExistsException extends ConflictDomainException {

    public GuardianNameAlreadyExistsException(String name) {
        // message only: the two-argument constructor fills ConflictDomainException's `id`, which the
        // mapper emits as parameters.id, and a name is not an id. This surface documents an error body
        // of message + http_status, so there is nothing else to put there.
        super("A Guardian named '" + name + "' already exists in this environment.");
    }
}
