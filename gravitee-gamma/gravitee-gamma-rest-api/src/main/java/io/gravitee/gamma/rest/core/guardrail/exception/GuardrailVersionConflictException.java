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
import io.gravitee.gamma.rest.core.guardrail.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import lombok.Getter;

/**
 * The draft changed since the caller read it, so the write was refused rather than applied over someone
 * else's work.
 *
 * <p>Carries the current Guardrail, not just its version: a client reloading after the refusal has what
 * to show without a second request, which would race with the next writer.
 *
 * @author GraviteeSource Team
 */
@Getter
public class GuardrailVersionConflictException extends ConflictDomainException {

    private final transient Guardrail current;

    public GuardrailVersionConflictException(Guardrail current, DraftPrecondition.OneOf required) {
        // message only, as the other Guardrail conflicts: this surface documents no parameters.id
        super(
            "Guardrail '%s' has been modified since it was read (version %s was required, the current version is %s).".formatted(
                current.getId(),
                required.versions(),
                current.getVersion()
            )
        );
        this.current = current;
    }
}
