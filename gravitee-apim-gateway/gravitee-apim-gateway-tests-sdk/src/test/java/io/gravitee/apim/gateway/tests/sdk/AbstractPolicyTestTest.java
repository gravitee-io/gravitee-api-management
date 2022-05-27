/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.gateway.tests.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.policy.api.PolicyConfiguration;
import java.lang.reflect.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class AbstractPolicyTestTest {

    @Test
    @DisplayName("Should register policy for first child")
    void shouldRegisterPolicyForFirstChild() {
        final ChildFirstLevel childFirstLevel = new ChildFirstLevel();
        final Type[] types = childFirstLevel.getClassGenericTypes();
        assertThat(types).hasSize(2);
        assertThat((Class<?>) types[0]).isEqualTo(FakePolicy.class);
        assertThat((Class<?>) types[1]).isEqualTo(FakePolicyConfiguration.class);
    }

    @Test
    @DisplayName("Should register policy for second child")
    void shouldRegisterPolicyForSecondChild() {
        final ChildSecondLevel childSecondLevel = new ChildSecondLevel();
        final Type[] types = childSecondLevel.getClassGenericTypes();
        assertThat(types).hasSize(2);
        assertThat((Class<?>) types[0]).isEqualTo(FakePolicy.class);
        assertThat((Class<?>) types[1]).isEqualTo(FakePolicyConfiguration.class);
    }

    class ChildFirstLevel extends AbstractPolicyTest<FakePolicy, FakePolicyConfiguration> {

        @Override
        protected String policyName() {
            return "first";
        }
    }

    class ChildSecondLevel extends ChildFirstLevel {

        @Override
        protected String policyName() {
            return "second";
        }
    }

    class FakePolicy {}

    class FakePolicyConfiguration implements PolicyConfiguration {}
}
