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

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * A Guardian whose sequence holds no enabled guardrail, refused at deployment.
 *
 * <p>One message for every way of getting there — an empty sequence, only disabled steps, a step
 * whose policy is not a guardrail or is no longer installed — because the caller has the same thing
 * to do in each case: put a guardrail the platform can run in the sequence and enable it.
 *
 * @author GraviteeSource Team
 */
public class GuardianWithoutEnabledGuardrailException extends ValidationDomainException {

    public GuardianWithoutEnabledGuardrailException(String guardianId) {
        super("Guardian '" + guardianId + "' needs at least one enabled guardrail to be deployed.");
    }
}
