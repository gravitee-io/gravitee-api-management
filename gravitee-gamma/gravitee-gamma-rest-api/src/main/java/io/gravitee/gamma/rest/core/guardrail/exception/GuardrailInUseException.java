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
 * A Guardrail change refused because APIs run it.
 *
 * <p>It counts them and does not name them. The message is built in the domain, which has no caller and therefore no
 * permissions to filter names against, and holding the Guardrail permission of an environment is not a right to read
 * every API of it. {@code GET /guardrails/{guardrailId}/apis} names the ones the caller may read, and that is where a
 * person goes to see which APIs to free first.
 *
 * @author GraviteeSource Team
 */
public class GuardrailInUseException extends ConflictDomainException {

    /** What an API running the Guardrail refuses. The wording of each lives here, where the message is built. */
    public enum RefusedChange {
        DELETION("deleted"),
        UNDEPLOYMENT("undeployed"),
        PHASE_OR_API_TYPES_CHANGE("moved to another phase or to other API types"),
        PUBLICATION_ELSEWHERE("deployed on another phase or on other API types than the revision its APIs run");

        private final String wording;

        RefusedChange(String wording) {
            this.wording = wording;
        }
    }

    /**
     * @param running how many APIs run the Guardrail
     */
    public GuardrailInUseException(String guardrailId, RefusedChange refusedChange, long running) {
        super(
            "Guardrail '" +
                guardrailId +
                "' cannot be " +
                refusedChange.wording +
                " while " +
                running +
                (running == 1 ? " API runs it" : " APIs run it") +
                ". List them with GET /guardrails/" +
                guardrailId +
                "/apis, then take it off each of them."
        );
    }
}
