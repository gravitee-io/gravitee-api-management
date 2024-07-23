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
package io.gravitee.gateway.handlers.sharedpolicygroup.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactor;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactorFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupRegistryTest {

    public static final String ID = "id";
    public static final String ENV_ID = "envId";

    @Mock
    private SharedPolicyGroupReactorFactory sharedPolicyGroupReactorFactory;

    @Mock
    private SharedPolicyGroupReactor sharedPolicyGroupReactor;

    private SharedPolicyGroupRegistry cut;

    @BeforeEach
    void setUp() {
        cut = new SharedPolicyGroupRegistry(sharedPolicyGroupReactorFactory);
    }

    @SneakyThrows
    @Test
    void should_create_shared_policy_group_reactor() {
        when(sharedPolicyGroupReactorFactory.create(any())).thenReturn(sharedPolicyGroupReactor);
        cut.create(ReactableSharedPolicyGroup.builder().id(ID).environmentId(ENV_ID).build());
        verify(sharedPolicyGroupReactor).start();
        assertThat(cut.get(ID, ENV_ID)).isSameAs(sharedPolicyGroupReactor);
    }

    @SneakyThrows
    @Test
    void should_create_shared_policy_group_reactor_and_stop_previous_one() {
        when(sharedPolicyGroupReactorFactory.create(any())).thenReturn(sharedPolicyGroupReactor);
        final SharedPolicyGroupReactor previousReactor = mock(SharedPolicyGroupReactor.class);
        cut.registry.put(new SharedPolicyGroupRegistry.SharedPolicyGroupRegistryKey(ID, ENV_ID), previousReactor);
        cut.create(ReactableSharedPolicyGroup.builder().id(ID).environmentId(ENV_ID).build());
        verify(sharedPolicyGroupReactor).start();
        verify(previousReactor).stop();
        assertThat(cut.get(ID, ENV_ID)).isSameAs(sharedPolicyGroupReactor);
    }

    @SneakyThrows
    @Test
    void should_remove_shared_policy_group_reactor() {
        final SharedPolicyGroupReactor previousReactor = mock(SharedPolicyGroupReactor.class);
        cut.registry.put(new SharedPolicyGroupRegistry.SharedPolicyGroupRegistryKey(ID, ENV_ID), previousReactor);
        cut.remove(ReactableSharedPolicyGroup.builder().id(ID).environmentId(ENV_ID).build());
        verify(previousReactor).stop();
        assertThat(cut.registry).isEmpty();
    }
}
