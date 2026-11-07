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
package io.gravitee.gateway.handlers.guardian.decision;

import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict.Transformation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * What the guardrails of one Guardian run concluded, in the order they ran. The policies of a chain run one after the
 * other, so a run has no concurrent access.
 *
 * @author GraviteeSource Team
 */
public class GuardianVerdicts {

    /**
     * From the one that lost the most of the original value to the one that lost the least: a rewrite leaves no trace of
     * the passage, a redaction says one was removed and by which rule, a mask keeps its length and its last characters.
     */
    private static final List<Transformation> MOST_DESTRUCTIVE_FIRST = List.of(
        Transformation.REWRITE,
        Transformation.REDACT,
        Transformation.MASK
    );

    private final List<Entry> entries = new ArrayList<>();

    /**
     * @param guardrail the name of the guardrail step.
     * @param verdict what it concluded.
     */
    public void record(String guardrail, GuardrailVerdict verdict) {
        entries.add(new Entry(guardrail, verdict));
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @return the most destructive transformation of the run, empty when no guardrail rewrote the exchange.
     */
    public Optional<Transformation> transformation() {
        return MOST_DESTRUCTIVE_FIRST.stream()
            .filter(transformation -> entries.stream().anyMatch(entry -> entry.verdict().transformation() == transformation))
            .findFirst();
    }

    public record Entry(String guardrail, GuardrailVerdict verdict) {}
}
