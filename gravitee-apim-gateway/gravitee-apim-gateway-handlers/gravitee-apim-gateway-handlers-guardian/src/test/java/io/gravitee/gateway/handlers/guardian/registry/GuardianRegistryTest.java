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
package io.gravitee.gateway.handlers.guardian.registry;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianRegistryTest {

    private static final String GUARDIAN_ID = "guardian-id";
    private static final String ENVIRONMENT_ID = "env-id";

    private final GuardianRegistry cut = new GuardianRegistry();

    @Test
    void should_return_null_when_no_reactor_is_registered() {
        assertThat(cut.get(GUARDIAN_ID, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_return_registered_reactor_for_its_guardian_and_environment() {
        GuardianReactor reactor = reactor(GUARDIAN_ID, ENVIRONMENT_ID);

        cut.register(reactor);

        assertThat(cut.get(GUARDIAN_ID, ENVIRONMENT_ID)).isSameAs(reactor);
        assertThat(cut.get(GUARDIAN_ID, "other-env")).isNull();
        assertThat(cut.get("other-guardian", ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_replace_previous_reactor_of_the_same_deployment() {
        GuardianReactor previous = reactor(GUARDIAN_ID, ENVIRONMENT_ID);
        GuardianReactor replacement = reactor(GUARDIAN_ID, ENVIRONMENT_ID);
        cut.register(previous);

        cut.register(replacement);

        assertThat(cut.get(GUARDIAN_ID, ENVIRONMENT_ID)).isSameAs(replacement);
    }

    @Test
    void should_unregister_a_reactor() {
        GuardianReactor reactor = reactor(GUARDIAN_ID, ENVIRONMENT_ID);
        cut.register(reactor);

        cut.unregister(reactor);

        assertThat(cut.get(GUARDIAN_ID, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_keep_the_replacement_when_the_replaced_reactor_is_unregistered_afterwards() {
        GuardianReactor previous = reactor(GUARDIAN_ID, ENVIRONMENT_ID);
        GuardianReactor replacement = reactor(GUARDIAN_ID, ENVIRONMENT_ID);
        cut.register(previous);
        cut.register(replacement);

        cut.unregister(previous);

        assertThat(cut.get(GUARDIAN_ID, ENVIRONMENT_ID)).isSameAs(replacement);
    }

    private static GuardianReactor reactor(String guardianId, String environmentId) {
        ReactableGuardian reactable = ReactableGuardian.builder().id(guardianId).environmentId(environmentId).build();
        return new GuardianReactor() {
            @Override
            public String id() {
                return guardianId;
            }

            @Override
            public ReactableGuardian reactableGuardian() {
                return reactable;
            }

            @Override
            public HttpPolicyChain policyChain() {
                return null;
            }
        };
    }
}
