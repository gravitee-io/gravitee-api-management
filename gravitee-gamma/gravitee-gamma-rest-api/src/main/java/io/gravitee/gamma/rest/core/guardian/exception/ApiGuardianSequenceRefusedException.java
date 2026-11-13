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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Collectors;

/**
 * A sequence of Guardians refused for more than one reason at once.
 *
 * <p>A sequence is validated whole, so a publisher who got three things wrong learns all three from one answer
 * instead of fixing one and sending the request again. The reasons do not mean the same thing to the caller, and the
 * message cannot be parsed, so {@link #getParameters()} keeps them apart: one entry per {@link Refusal} present,
 * holding the ids it applies to.
 *
 * <p>A single fault answers as itself — {@code GuardianNotFoundException} and the others — and not as a list of one.
 * This exception is the answer to a request that is wrong in several ways, and only that.
 *
 * @author GraviteeSource Team
 */
public class ApiGuardianSequenceRefusedException extends ValidationDomainException {

    /** What is wrong with a Guardian of the sequence, and the parameter key under which its ids travel. */
    public enum Refusal {
        /**
         * The environment holds no such Guardian. On its own this answers {@code GuardianNotFoundException}, a
         * {@code NotFoundDomainException}; here it travels with faults of another nature, and the whole answer is a
         * validation one.
         */
        NOT_FOUND("notFoundGuardianIds", "the environment holds no such Guardian"),
        NOT_DEPLOYED("notDeployedGuardianIds", "no revision of them is deployed"),
        INCOMPATIBLE_API_TYPE("incompatibleApiTypeGuardianIds", "their deployed revision does not support this API's type"),
        PHASE_MISMATCH("phaseMismatchGuardianIds", "their deployed revision runs in the other phase"),
        LISTED_TWICE("duplicateGuardianIds", "they are listed twice on the same phase");

        private final String parameterKey;
        private final String wording;

        Refusal(String parameterKey, String wording) {
            this.parameterKey = parameterKey;
            this.wording = wording;
        }

        public String parameterKey() {
            return parameterKey;
        }
    }

    /**
     * @param idsByRefusal the ids of each refusal, in the order the refusals are reported. Only the refusals present
     *     appear, each id once per refusal; an id refused for two different reasons — one per phase — appears under
     *     both.
     */
    public ApiGuardianSequenceRefusedException(SequencedMap<Refusal, SequencedSet<String>> idsByRefusal) {
        super(message(idsByRefusal), parameters(idsByRefusal));
    }

    private static String message(SequencedMap<Refusal, SequencedSet<String>> idsByRefusal) {
        return (
            "This sequence of Guardians cannot be set on the API. " +
            idsByRefusal
                .entrySet()
                .stream()
                .map(entry -> "[" + String.join(", ", entry.getValue()) + "]: " + entry.getKey().wording)
                .collect(Collectors.joining(". ")) +
            "."
        );
    }

    private static Map<String, String> parameters(SequencedMap<Refusal, SequencedSet<String>> idsByRefusal) {
        var parameters = new LinkedHashMap<String, String>();
        idsByRefusal.forEach((refusal, ids) -> parameters.put(refusal.parameterKey(), String.join(",", ids)));
        return parameters;
    }
}
