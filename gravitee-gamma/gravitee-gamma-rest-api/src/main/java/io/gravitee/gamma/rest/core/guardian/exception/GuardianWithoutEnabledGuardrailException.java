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
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Guardian whose sequence holds no enabled guardrail, refused at deployment.
 *
 * <p>The sequence may be empty, hold only disabled steps, or hold enabled steps that no installed
 * guardrail backs. The last case is the one that needs more than the rule to act on: the user sees
 * an enabled guardrail and has to be told which step the platform cannot run, so the message names
 * the offending steps when there are any.
 *
 * @author GraviteeSource Team
 */
public class GuardianWithoutEnabledGuardrailException extends ValidationDomainException {

    /**
     * @param stepsThatAreNoGuardrail the enabled steps no installed guardrail backs, empty when the
     *     sequence has no enabled step at all — there is then nothing to point at.
     */
    public GuardianWithoutEnabledGuardrailException(String guardianId, List<Step> stepsThatAreNoGuardrail) {
        super(message(guardianId, stepsThatAreNoGuardrail));
    }

    private static String message(String guardianId, List<Step> stepsThatAreNoGuardrail) {
        var rule = "Guardian '" + guardianId + "' needs at least one enabled guardrail to be deployed";
        if (stepsThatAreNoGuardrail.isEmpty()) {
            return rule + ".";
        }
        return (
            rule +
            ": " +
            stepsThatAreNoGuardrail
                .stream()
                .map(step -> "step [%s] uses policy [%s], which is not an installed guardrail".formatted(step.getName(), step.getPolicy()))
                .collect(Collectors.joining(", ")) +
            "."
        );
    }
}
