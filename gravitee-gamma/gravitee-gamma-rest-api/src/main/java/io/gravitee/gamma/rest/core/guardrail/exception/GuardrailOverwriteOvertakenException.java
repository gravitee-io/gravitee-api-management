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
 * A write that replaces the draft whatever it holds — a rollback, or an update asking to overwrite — was
 * overtaken by other writers on each of its attempts, and gave up rather than write blindly.
 *
 * <p>A plain conflict, answered 409: the caller stated no version, so no precondition failed, which is
 * what a 412 would say. Retrying is the remedy.
 *
 * @author GraviteeSource Team
 */
public class GuardrailOverwriteOvertakenException extends ConflictDomainException {

    public GuardrailOverwriteOvertakenException(String guardrailId) {
        // message only, as the other Guardrail conflicts: this surface documents no parameters.id
        super("Guardrail '%s' is being modified by other writers and could not be replaced: try again.".formatted(guardrailId));
    }
}
