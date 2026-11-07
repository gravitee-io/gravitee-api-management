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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict.Transformation;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianVerdictsTest {

    private final GuardianVerdicts cut = new GuardianVerdicts();

    @Test
    void should_be_empty_until_a_verdict_is_recorded() {
        assertThat(cut.isEmpty()).isTrue();
        assertThat(cut.entries()).isEmpty();

        cut.record("step", allow());

        assertThat(cut.isEmpty()).isFalse();
    }

    @Test
    void should_keep_the_verdicts_in_the_order_they_were_recorded() {
        GuardrailVerdict first = allow();
        GuardrailVerdict second = GuardrailVerdict.deny("BLOCK", List.of(), List.of());

        cut.record("first", first);
        cut.record("second", second);

        assertThat(cut.entries()).containsExactly(new GuardianVerdicts.Entry("first", first), new GuardianVerdicts.Entry("second", second));
    }

    @Test
    void should_have_no_transformation_without_a_transform_verdict() {
        cut.record("step", allow());

        assertThat(cut.transformation()).isEmpty();
    }

    @Test
    void should_prefer_rewrite_over_redact_and_mask() {
        cut.record("mask", transform(Transformation.MASK));
        cut.record("redact", transform(Transformation.REDACT));
        cut.record("rewrite", transform(Transformation.REWRITE));

        assertThat(cut.transformation()).contains(Transformation.REWRITE);
    }

    @Test
    void should_prefer_redact_over_mask() {
        cut.record("mask", transform(Transformation.MASK));
        cut.record("redact", transform(Transformation.REDACT));

        assertThat(cut.transformation()).contains(Transformation.REDACT);
    }

    private static GuardrailVerdict allow() {
        return GuardrailVerdict.allow("CONTINUE", List.of(), List.of());
    }

    private static GuardrailVerdict transform(Transformation transformation) {
        return GuardrailVerdict.transform(transformation, transformation.name(), List.of(), List.of());
    }
}
