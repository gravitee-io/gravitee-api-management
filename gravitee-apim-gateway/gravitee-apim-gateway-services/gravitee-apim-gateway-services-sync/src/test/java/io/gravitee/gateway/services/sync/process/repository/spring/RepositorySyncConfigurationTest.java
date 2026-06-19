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
package io.gravitee.gateway.services.sync.process.repository.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.GammaEnabledCondition;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

class RepositorySyncConfigurationTest {

    @Test
    void declares_authz_pdp_mapper_bean_under_gamma_condition() {
        Method bean = beanMethod("authzPdpMapper");
        assertThat(bean).isNotNull();
        assertThat(isGammaConditional(bean)).isTrue();
    }

    @Test
    void declares_authz_pdp_synchronizer_bean_under_gamma_condition() {
        Method bean = beanMethod("authzPdpSynchronizer");
        assertThat(bean).isNotNull();
        assertThat(isGammaConditional(bean)).isTrue();
    }

    @Test
    void does_not_declare_replay_responder_bean() {
        assertThat(beanMethod("authzReplayResponder")).isNull();
    }

    private static Method beanMethod(String name) {
        return Arrays.stream(RepositorySyncConfiguration.class.getDeclaredMethods())
            .filter(m -> m.getName().equals(name) && m.isAnnotationPresent(Bean.class))
            .findFirst()
            .orElse(null);
    }

    private static boolean isGammaConditional(Method method) {
        Conditional conditional = method.getAnnotation(Conditional.class);
        return conditional != null && Arrays.asList(conditional.value()).contains(GammaEnabledCondition.class);
    }
}
