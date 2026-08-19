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
package io.gravitee.apim.rest.api.automation.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.gamma.api.automation.GammaAutomationPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GammaAutomationPortsTest {

    private GenericApplicationContext context;
    private GammaAutomationPorts ports;

    @BeforeEach
    void setUp() {
        context = new GenericApplicationContext();
        context.refresh();
        ports = new GammaAutomationPorts(context.getBeanProvider(GammaAutomationPort.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_return_empty_when_no_port_bean() {
        assertThat(ports.module("aim")).isEmpty();
        assertThat(ports.all()).isEmpty();
    }

    @Test
    void should_find_port_by_module_id() {
        registerModulePort("aim");
        registerModulePort("authz");

        assertThat(ports.module("aim")).hasValueSatisfying(port -> assertThat(port.module()).isEqualTo("aim"));
        assertThat(ports.module("authz")).hasValueSatisfying(port -> assertThat(port.module()).isEqualTo("authz"));
        assertThat(ports.module("esm")).isEmpty();
        assertThat(ports.all()).hasSize(2);
    }

    @Test
    void should_see_port_registered_after_creation() {
        // Modules are loaded by the plugin handler after the host contexts exist: the lookup must be lazy.
        assertThat(ports.module("aim")).isEmpty();

        registerModulePort("aim");

        assertThat(ports.module("aim")).isPresent();
    }

    @Test
    void should_fail_loudly_when_two_ports_claim_the_same_module() {
        registerModulePort("aim");
        registerModulePort("aim");

        assertThatThrownBy(() -> ports.module("aim"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("aim");
    }

    private void registerModulePort(String module) {
        // Same registration the plugin handler performs: "<pluginId>.<beanName>" singleton in the root context.
        var port = mock(GammaAutomationPort.class);
        when(port.module()).thenReturn(module);
        context.getBeanFactory().registerSingleton(module + ".automationPort" + System.identityHashCode(port), port);
    }
}
