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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.guardrail.GuardrailDecisionReasonLine.Parsed;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuardrailDecisionReasonLineTest {

    @Test
    void writes_the_guardrail_policy_its_verdict_and_its_reasons() {
        assertThat(GuardrailDecisionReasonLine.format("Block credentials", "BLOCK", List.of("Blocked", "matched"))).isEqualTo(
            "Block credentials: BLOCK — Blocked; matched"
        );
        assertThat(GuardrailDecisionReasonLine.format("Topic check", "CONTINUE", List.of())).isEqualTo("Topic check: CONTINUE");
    }

    /** What the gateway writes, the management API reads back: the two cannot drift apart. */
    @Test
    void reads_back_what_it_writes() {
        var line = GuardrailDecisionReasonLine.format("Block credentials", "BLOCK", List.of("Blocked", "matched"));

        assertThat(GuardrailDecisionReasonLine.parse(line, Set.of())).isEqualTo(
            new Parsed("Block credentials", "BLOCK", "Blocked; matched")
        );
        assertThat(
            GuardrailDecisionReasonLine.parse(GuardrailDecisionReasonLine.format("Topic check", "CONTINUE", List.of()), Set.of())
        ).isEqualTo(new Parsed("Topic check", "CONTINUE", null));
    }

    @Test
    void reads_a_known_name_holding_the_separator_whole() {
        var line = GuardrailDecisionReasonLine.format("PII: email", "REDACT", List.of("1 passage"));

        assertThat(GuardrailDecisionReasonLine.parse(line, Set.of("PII", "PII: email"))).isEqualTo(
            new Parsed("PII: email", "REDACT", "1 passage")
        );
    }

    @Test
    void keeps_a_line_it_cannot_split_as_the_name() {
        assertThat(GuardrailDecisionReasonLine.parse("unexpected", Set.of())).isEqualTo(new Parsed("unexpected", null, null));
    }
}
