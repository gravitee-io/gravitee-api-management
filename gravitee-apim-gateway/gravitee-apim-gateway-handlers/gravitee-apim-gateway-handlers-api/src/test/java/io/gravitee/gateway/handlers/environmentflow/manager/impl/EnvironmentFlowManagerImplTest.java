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
package io.gravitee.gateway.handlers.environmentflow.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.LicenseManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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
class EnvironmentFlowManagerImplTest {

    public static final String ENV_FLOW_ID = "env-flow-id";
    private EnvironmentFlowManagerImpl cut;

    @Mock
    private EventManager eventManager;

    @Mock
    private LicenseManager licenseManager;

    @BeforeEach
    void setUp() {
        cut = new EnvironmentFlowManagerImpl(eventManager, licenseManager);
    }

    @Test
    void should_deploy_environment_flow() {
        final ReactableEnvironmentFlow environmentFlow = new EnvironmentFlowBuilder().id(ENV_FLOW_ID).build();
        cut.register(environmentFlow);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentFlow);
        assertThat(cut.environmentFlows()).hasSize(1);
    }

    @Test
    void should_update_environment_flow() {
        final ReactableEnvironmentFlow environmentFlow = new EnvironmentFlowBuilder().id(ENV_FLOW_ID).build();
        environmentFlow.setDeployedAt(new Date());
        cut.register(environmentFlow);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentFlow);
        assertThat(cut.environmentFlows()).hasSize(1);

        final ReactableEnvironmentFlow environmentFlow2 = new EnvironmentFlowBuilder().id(ENV_FLOW_ID).build();
        Instant deployDateInst = environmentFlow.getDeployedAt().toInstant().plus(Duration.ofHours(1));
        environmentFlow2.setDeployedAt(Date.from(deployDateInst));

        cut.register(environmentFlow2);
        verify(eventManager).publishEvent(ReactorEvent.UPDATE, environmentFlow2);
        assertThat(cut.environmentFlows()).hasSize(1);
    }

    @Test
    void should_not_update_environment_flow() {
        final ReactableEnvironmentFlow environmentFlow = new EnvironmentFlowBuilder().id(ENV_FLOW_ID).build();
        environmentFlow.setDeployedAt(new Date());
        cut.register(environmentFlow);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentFlow);
        assertThat(cut.environmentFlows()).hasSize(1);

        final ReactableEnvironmentFlow environmentFlow2 = new EnvironmentFlowBuilder().id(ENV_FLOW_ID).build();
        Instant deployDateInst = environmentFlow.getDeployedAt().toInstant().minus(Duration.ofHours(1));
        environmentFlow2.setDeployedAt(Date.from(deployDateInst));

        cut.register(environmentFlow2);
        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, environmentFlow2);
        assertThat(cut.environmentFlows()).hasSize(1);
    }

    @Test
    void should_undeploy_environment_flow() {
        final ReactableEnvironmentFlow environmentFlow = new EnvironmentFlowBuilder().id(ENV_FLOW_ID).build();
        environmentFlow.setDeployedAt(new Date());
        cut.register(environmentFlow);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, environmentFlow);
        assertThat(cut.environmentFlows()).hasSize(1);

        cut.unregister(environmentFlow.getId());
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, environmentFlow);
        assertThat(cut.environmentFlows()).isEmpty();
    }

    static class EnvironmentFlowBuilder {

        private final EnvironmentFlow definition = new EnvironmentFlow();
        private final ReactableEnvironmentFlow reactableEnvironmentFlow = new ReactableEnvironmentFlow();

        {
            reactableEnvironmentFlow.setDefinition(definition);
        }

        public EnvironmentFlowManagerImplTest.EnvironmentFlowBuilder id(String id) {
            reactableEnvironmentFlow.setId(id);
            this.definition.setId(id);
            return this;
        }

        public EnvironmentFlowManagerImplTest.EnvironmentFlowBuilder name(String name) {
            this.definition.setName(name);
            return this;
        }

        public ReactableEnvironmentFlow build() {
            return this.reactableEnvironmentFlow;
        }
    }
}
