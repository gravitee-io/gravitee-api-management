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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

class GammaConditionsTest {

    @Test
    void gamma_enabled_condition_matches_when_property_is_true() {
        assertThat(new GammaEnabledCondition().matches(ctxWithGamma("true"), null)).isTrue();
    }

    @Test
    void gamma_enabled_condition_does_not_match_when_property_is_absent_or_false() {
        assertThat(new GammaEnabledCondition().matches(ctxWithGamma(null), null)).isFalse();
        assertThat(new GammaEnabledCondition().matches(ctxWithGamma("false"), null)).isFalse();
    }

    @Test
    void gamma_disabled_condition_is_the_inverse_so_exactly_one_AuthzEnginePort_bean_is_registered() {
        // The two conditions are paired — at any time exactly one of {EventBus, Noop} must bind,
        // otherwise the DeployerFactory wiring is ambiguous or absent.
        for (String value : new String[] { null, "false", "true" }) {
            ConditionContext ctx = ctxWithGamma(value);
            boolean enabled = new GammaEnabledCondition().matches(ctx, null);
            boolean disabled = new GammaDisabledCondition().matches(ctx, null);
            assertThat(enabled).as("gamma.enabled=%s — enabled and disabled conditions must be inverse", value).isNotEqualTo(disabled);
        }
    }

    private static ConditionContext ctxWithGamma(String value) {
        ConditionContext ctx = mock(ConditionContext.class);
        Environment env = mock(Environment.class);
        when(ctx.getEnvironment()).thenReturn(env);
        Boolean parsed = value == null ? null : Boolean.parseBoolean(value);
        when(env.getProperty("gamma.enabled", Boolean.class, false)).thenReturn(parsed != null ? parsed : false);
        return ctx;
    }
}
