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
package io.gravitee.gateway.handlers.guardrail.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.decision.GuardrailVerdicts;
import io.gravitee.gateway.handlers.guardrail.reactor.GuardrailReactor;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailRegistryTest {

    private static final String GUARDRAIL_ID = "guardrail-id";
    private static final String ENVIRONMENT_ID = "env-id";

    private final GuardrailRegistry cut = new GuardrailRegistry();

    @Test
    void should_return_null_when_no_reactor_is_registered() {
        assertThat(cut.get(GUARDRAIL_ID, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_return_registered_reactor_for_its_guardrail_and_environment() {
        GuardrailReactor reactor = reactor(GUARDRAIL_ID, ENVIRONMENT_ID);

        cut.register(reactor);

        assertThat(cut.get(GUARDRAIL_ID, ENVIRONMENT_ID)).isSameAs(reactor);
        assertThat(cut.get(GUARDRAIL_ID, "other-env")).isNull();
        assertThat(cut.get("other-guardrail", ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_replace_previous_reactor_of_the_same_deployment() {
        GuardrailReactor previous = reactor(GUARDRAIL_ID, ENVIRONMENT_ID);
        GuardrailReactor replacement = reactor(GUARDRAIL_ID, ENVIRONMENT_ID);
        cut.register(previous);

        cut.register(replacement);

        assertThat(cut.get(GUARDRAIL_ID, ENVIRONMENT_ID)).isSameAs(replacement);
    }

    @Test
    void should_unregister_a_reactor() {
        GuardrailReactor reactor = reactor(GUARDRAIL_ID, ENVIRONMENT_ID);
        cut.register(reactor);

        cut.unregister(reactor);

        assertThat(cut.get(GUARDRAIL_ID, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_keep_the_replacement_when_the_replaced_reactor_is_unregistered_afterwards() {
        GuardrailReactor previous = reactor(GUARDRAIL_ID, ENVIRONMENT_ID);
        GuardrailReactor replacement = reactor(GUARDRAIL_ID, ENVIRONMENT_ID);
        cut.register(previous);
        cut.register(replacement);

        cut.unregister(previous);

        assertThat(cut.get(GUARDRAIL_ID, ENVIRONMENT_ID)).isSameAs(replacement);
    }

    @Test
    void should_reject_a_reactor_without_guardrail() {
        GuardrailReactor reactor = reactor(null);

        assertThatThrownBy(() -> cut.register(reactor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no Guardrail");
        assertThat(cut.get(GUARDRAIL_ID, null)).isNull();
    }

    private static GuardrailReactor reactor(String guardrailId, String environmentId) {
        return reactor(new ReactableGuardrail(Guardrail.builder().id(guardrailId).environmentId(environmentId).build()));
    }

    private static GuardrailReactor reactor(ReactableGuardrail reactable) {
        return new GuardrailReactor() {
            @Override
            public ReactableGuardrail reactableGuardrail() {
                return reactable;
            }

            @Override
            public Completable execute(HttpPlainExecutionContext ctx, BasePolicy source, GuardrailVerdicts verdicts) {
                return Completable.complete();
            }
        };
    }
}
