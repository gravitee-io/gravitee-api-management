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
package io.gravitee.gateway.handlers.sharedpolicygroup.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import java.util.List;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupReactorFactoryManagerTest {

    SharedPolicyGroupReactorFactoryManager cut;

    @Test
    void should_create_reactable_using_the_corresponding_factory() {
        cut =
            new SharedPolicyGroupReactorFactoryManager(
                List.of(new FakeSharedPolicyGroupReactorFactory("fake-1"), new FakeSharedPolicyGroupReactorFactory("fake-2"))
            );
        final SharedPolicyGroupReactor expectedFake1 = cut.create(
            new ReactableSharedPolicyGroup(SharedPolicyGroup.builder().id("fake-1").build())
        );
        assertThat(expectedFake1.id()).isEqualTo("fake-1");
        final SharedPolicyGroupReactor expectedFake2 = cut.create(
            new ReactableSharedPolicyGroup(SharedPolicyGroup.builder().id("fake-2").build())
        );
        assertThat(expectedFake2.id()).isEqualTo("fake-2");
    }

    @Test
    void should_create_reactable_using_the_corresponding_factory_with_one_factory_registered_manually() {
        cut = new SharedPolicyGroupReactorFactoryManager(List.of(new FakeSharedPolicyGroupReactorFactory("fake-1")));
        final SharedPolicyGroupReactor expectedFake1 = cut.create(
            new ReactableSharedPolicyGroup(SharedPolicyGroup.builder().id("fake-1").build())
        );
        assertThat(expectedFake1.id()).isEqualTo("fake-1");
        cut.register(new FakeSharedPolicyGroupReactorFactory("fake-2"));
        final SharedPolicyGroupReactor expectedFake2 = cut.create(
            new ReactableSharedPolicyGroup(SharedPolicyGroup.builder().id("fake-2").build())
        );
        assertThat(expectedFake2.id()).isEqualTo("fake-2");
    }

    @Test
    void should_throw_if_no_corresponding_factory() {
        cut = new SharedPolicyGroupReactorFactoryManager(List.of());
        assertThatThrownBy(() -> cut.create(new ReactableSharedPolicyGroup(SharedPolicyGroup.builder().id("fake-1").build())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No reactor found for reactable: fake-1");
        cut.register(new FakeSharedPolicyGroupReactorFactory("fake-2"));
        final SharedPolicyGroupReactor expectedFake2 = cut.create(
            new ReactableSharedPolicyGroup(SharedPolicyGroup.builder().id("fake-2").build())
        );
        assertThat(expectedFake2.id()).isEqualTo("fake-2");
    }

    @AllArgsConstructor
    static class FakeSharedPolicyGroupReactorFactory implements SharedPolicyGroupReactorFactory {

        private final String supportId;

        @Override
        public boolean canCreate(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
            return supportId.equals(reactableSharedPolicyGroup.getId());
        }

        @Override
        public SharedPolicyGroupReactor create(ReactableSharedPolicyGroup reactableSharedPolicyGroup) {
            return new SharedPolicyGroupReactor() {
                @Override
                public String id() {
                    return supportId;
                }

                @Override
                public ReactableSharedPolicyGroup reactableSharedPolicyGroup() {
                    return null;
                }

                @Override
                public HttpPolicyChain policyChain() {
                    return null;
                }

                @Override
                public Lifecycle.State lifecycleState() {
                    return null;
                }

                @Override
                public SharedPolicyGroupReactor start() throws Exception {
                    return null;
                }

                @Override
                public SharedPolicyGroupReactor stop() throws Exception {
                    return null;
                }
            };
        }
    }
}
