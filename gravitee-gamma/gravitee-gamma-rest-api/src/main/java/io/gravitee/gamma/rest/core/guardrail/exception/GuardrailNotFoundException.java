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

import io.gravitee.apim.core.exception.NotFoundDomainException;
import java.util.SequencedSet;
import java.util.stream.Collectors;

/**
 * No Guardrail with this id in this environment.
 *
 * <p>Also thrown when the Guardrail exists but belongs to another environment: the caller is told the
 * same thing either way, so a 404 never confirms that an id exists somewhere else.
 *
 * @author GraviteeSource Team
 */
public class GuardrailNotFoundException extends NotFoundDomainException {

    public GuardrailNotFoundException(String guardrailId) {
        super("Guardrail '" + guardrailId + "' not found", guardrailId);
    }

    /**
     * Several at once, which is what validating a whole sequence reports: a publisher who named three Guardrails the
     * environment does not hold learns all three from one answer. One id reads exactly as the single-id form does,
     * {@link #getId()} included — only a list of several leaves it unset, since no single id is <em>the</em> one.
     */
    public static GuardrailNotFoundException of(SequencedSet<String> guardrailIds) {
        if (guardrailIds.size() == 1) {
            return new GuardrailNotFoundException(guardrailIds.getFirst());
        }
        var named = guardrailIds
            .stream()
            .map(id -> "'" + id + "'")
            .collect(Collectors.joining(", "));
        return new GuardrailNotFoundException("Guardrails not found: " + named, null);
    }

    private GuardrailNotFoundException(String message, String id) {
        super(message, id);
    }
}
