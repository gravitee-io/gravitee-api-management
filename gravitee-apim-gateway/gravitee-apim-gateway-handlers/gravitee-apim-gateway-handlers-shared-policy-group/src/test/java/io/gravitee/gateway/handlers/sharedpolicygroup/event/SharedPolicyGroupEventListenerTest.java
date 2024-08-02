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
package io.gravitee.gateway.handlers.sharedpolicygroup.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.registry.SharedPolicyGroupRegistry;
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
class SharedPolicyGroupEventListenerTest {

    @Mock
    EventManager eventManager;

    @Mock
    SharedPolicyGroupRegistry sharedPolicyGroupRegistry;

    private SharedPolicyGroupEventListener cut;

    @BeforeEach
    void setUp() {
        cut = new SharedPolicyGroupEventListener(eventManager, sharedPolicyGroupRegistry);
    }

    @Test
    void should_deploy() {
        ReactableSharedPolicyGroup content = new ReactableSharedPolicyGroup();
        cut.onEvent(new SimpleEvent<>(SharedPolicyGroupEvent.DEPLOY, content));
        verify(sharedPolicyGroupRegistry).create(content);
    }

    @Test
    void should_undeploy() {
        ReactableSharedPolicyGroup content = new ReactableSharedPolicyGroup();
        cut.onEvent(new SimpleEvent<>(SharedPolicyGroupEvent.UNDEPLOY, content));
        verify(sharedPolicyGroupRegistry).remove(content);
    }

    @Test
    void should_update() {
        ReactableSharedPolicyGroup content = new ReactableSharedPolicyGroup();
        cut.onEvent(new SimpleEvent<>(SharedPolicyGroupEvent.UPDATE, content));
        verify(sharedPolicyGroupRegistry).update(content);
    }
}
