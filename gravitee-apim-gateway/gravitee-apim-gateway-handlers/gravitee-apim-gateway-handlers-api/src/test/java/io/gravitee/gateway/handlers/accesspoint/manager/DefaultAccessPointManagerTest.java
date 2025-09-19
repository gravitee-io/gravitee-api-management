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
package io.gravitee.gateway.handlers.accesspoint.manager;

import static io.gravitee.gateway.reactor.accesspoint.AccessPointEvent.DEPLOY;
import static io.gravitee.gateway.reactor.accesspoint.AccessPointEvent.UNDEPLOY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultAccessPointManagerTest {

    @Mock
    private EventManager eventManager;

    private DefaultAccessPointManager cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DefaultAccessPointManager(eventManager);
    }

    @Test
    void should_register_new_access_point() {
        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .host("host")
            .environmentId("environmentId")
            .build();
        cut.register(reactableAccessPoint);

        assertThat(cut.getByEnvironmentId("environmentId")).containsOnly(reactableAccessPoint);
        verify(eventManager).publishEvent(DEPLOY, reactableAccessPoint);
    }

    @Test
    void should_not_register_duplicated_access_point() {
        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .host("host")
            .environmentId("environmentId")
            .build();
        cut.register(reactableAccessPoint);
        cut.register(reactableAccessPoint);

        assertThat(cut.getByEnvironmentId("environmentId")).containsOnly(reactableAccessPoint);
        verify(eventManager).publishEvent(DEPLOY, reactableAccessPoint);
    }

    @Test
    void should_unregister_new_access_point() {
        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .host("host")
            .environmentId("environmentId")
            .build();
        cut.register(reactableAccessPoint);
        assertThat(cut.getByEnvironmentId("environmentId")).containsOnly(reactableAccessPoint);
        verify(eventManager).publishEvent(DEPLOY, reactableAccessPoint);
        cut.unregister(reactableAccessPoint);
        assertThat(cut.getByEnvironmentId("environmentId")).isEmpty();
        verify(eventManager).publishEvent(UNDEPLOY, reactableAccessPoint);
    }

    @Test
    void should_not_unregister_unexisting_access_point() {
        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .host("host")
            .environmentId("environmentId")
            .build();
        cut.unregister(reactableAccessPoint);

        assertThat(cut.getByEnvironmentId("environmentId")).isEmpty();
        verify(eventManager, never()).publishEvent(UNDEPLOY, reactableAccessPoint);
    }
}
