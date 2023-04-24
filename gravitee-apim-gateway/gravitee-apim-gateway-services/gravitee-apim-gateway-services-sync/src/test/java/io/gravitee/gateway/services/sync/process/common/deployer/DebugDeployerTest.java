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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.debug.DebugDeployable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DebugDeployerTest {

    @Mock
    private EventManager eventManager;

    private DebugDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DebugDeployer(eventManager);
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_debug_reactable() {
            Reactable reactable = mock(Reactable.class);
            DebugDeployable deployable = DebugDeployable.builder().id("id").reactable(reactable).build();
            cut.deploy(deployable).test().assertComplete();
            verify(eventManager).publishEvent(ReactorEvent.DEBUG, reactable);
            verifyNoMoreInteractions(eventManager);
        }

        @Test
        void should_return_error_when_api_manager_throw_exception() {
            Reactable reactable = mock(Reactable.class);
            DebugDeployable deployable = DebugDeployable.builder().id("id").reactable(reactable).build();
            doThrow(new SyncException("error")).when(eventManager).publishEvent(ReactorEvent.DEBUG, reactable);
            cut.deploy(deployable).test().assertFailure(SyncException.class);
            verify(eventManager).publishEvent(ReactorEvent.DEBUG, reactable);
            verifyNoMoreInteractions(eventManager);
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(eventManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_do_nothing() {
            cut.undeploy(null).test().assertComplete();
            verifyNoMoreInteractions(eventManager);
        }
    }
}
