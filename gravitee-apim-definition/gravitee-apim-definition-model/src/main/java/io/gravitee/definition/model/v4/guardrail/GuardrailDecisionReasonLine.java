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
package io.gravitee.definition.model.v4.guardrail;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The line a Guardrail decision records for each guardrail policy that reached a verdict:
 * {@code "<guardrail policy>: <verdict>"}, followed by {@code " — <reason>; <reason>"} when the policy gave reasons.
 *
 * <p>The gateway writes it and the management API reads it back into steps: both go through this class, so a change
 * of format reaches the reader in the same build instead of silently degrading what it serves.
 *
 * @author GraviteeSource Team
 */
public final class GuardrailDecisionReasonLine {

    /** Ends the guardrail policy name. */
    public static final String POLICY_SEPARATOR = ": ";
    /** Ends the verdict, when the guardrail policy gave reasons. */
    public static final String REASON_SEPARATOR = " — ";
    /** Joins the reasons of one guardrail policy. */
    public static final String REASONS_DELIMITER = "; ";

    private GuardrailDecisionReasonLine() {}

    /**
     * One line read back.
     *
     * @param verdict {@code null} for a line that cannot be split, kept whole as the name.
     * @param reason the reasons as written, joined; {@code null} when the guardrail policy gave none.
     */
    public record Parsed(String guardrailPolicy, String verdict, String reason) {}

    public static String format(String guardrailPolicy, String verdict, List<String> reasons) {
        String line = guardrailPolicy + POLICY_SEPARATOR + verdict;
        return reasons == null || reasons.isEmpty() ? line : line + REASON_SEPARATOR + String.join(REASONS_DELIMITER, reasons);
    }

    /**
     * A guardrail policy name may hold {@code ": "} itself: the names the caller already knows (from the findings of the
     * same decision) are matched first, longest first, so a name is cut at its first {@code ": "} only when unknown.
     */
    public static Parsed parse(String line, Collection<String> knownPolicies) {
        int separator = knownPolicies
            .stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .filter(policy -> line.startsWith(policy + POLICY_SEPARATOR))
            .findFirst()
            .map(String::length)
            .orElse(line.indexOf(POLICY_SEPARATOR));
        if (separator < 0) {
            return new Parsed(line, null, null);
        }
        String rest = line.substring(separator + POLICY_SEPARATOR.length());
        int dash = rest.indexOf(REASON_SEPARATOR);
        return dash < 0
            ? new Parsed(line.substring(0, separator), rest, null)
            : new Parsed(line.substring(0, separator), rest.substring(0, dash), rest.substring(dash + REASON_SEPARATOR.length()));
    }
}
