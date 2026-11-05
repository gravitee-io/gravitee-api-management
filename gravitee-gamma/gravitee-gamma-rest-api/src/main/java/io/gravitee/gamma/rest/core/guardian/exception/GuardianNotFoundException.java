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

import io.gravitee.apim.core.exception.NotFoundDomainException;

/**
 * No Guardian with this id in this environment.
 *
 * <p>Also thrown when the Guardian exists but belongs to another environment: the caller is told the
 * same thing either way, so a 404 never confirms that an id exists somewhere else.
 *
 * @author GraviteeSource Team
 */
public class GuardianNotFoundException extends NotFoundDomainException {

    public GuardianNotFoundException(String guardianId) {
        super("Guardian '" + guardianId + "' not found", guardianId);
    }
}
