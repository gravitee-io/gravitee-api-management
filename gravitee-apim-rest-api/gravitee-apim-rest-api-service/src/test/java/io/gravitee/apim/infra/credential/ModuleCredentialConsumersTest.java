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
package io.gravitee.gamma.module.platform.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.apim.plugin.gamma.api.credential.CredentialConsumer;
import io.gravitee.apim.plugin.gamma.api.credential.CredentialConsumerPort;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialConsumers;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Wires the collector the way the plugin contexts are wired: the Platform context is a child of the host context, and a
 * loaded module's beans are copied into the host context under a {@code <module>.<bean>} name.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ModuleCredentialConsumersTest {

    private GenericApplicationContext host;
    private GenericApplicationContext platform;

    record FakePort(String module, Map<String, List<CredentialConsumer>> byCredential) implements CredentialConsumerPort {
        @Override
        public List<CredentialConsumer> consumersOf(String environmentId, String credentialId) {
            return byCredential.getOrDefault(environmentId + "/" + credentialId, List.of());
        }
    }

    @BeforeEach
    void setUp() {
        host = new GenericApplicationContext();
        host.refresh();
        platform = new GenericApplicationContext();
        platform.setParent(host);
        platform.registerBean(ModuleCredentialConsumers.class, () ->
            new ModuleCredentialConsumers(platform.getBeanProvider(CredentialConsumerPort.class))
        );
        platform.refresh();
    }

    @AfterEach
    void tearDown() {
        platform.close();
        host.close();
    }

    private CredentialConsumers collector() {
        return platform.getBean(CredentialConsumers.class);
    }

    private void loadModule(String beanName, CredentialConsumerPort port) {
        host.getBeanFactory().registerSingleton(beanName, port);
    }

    private static CredentialConsumer proxy(String id, String name) {
        return new CredentialConsumer("LLM_PROXY", id, name);
    }

    @Test
    void should_find_no_consumer_when_no_module_reports_any() {
        assertThat(collector().of("env-1", "credential-1")).isEmpty();
    }

    @Test
    void should_ask_a_module_loaded_after_the_platform_context() {
        loadModule("aim.credentialConsumers", new FakePort("aim", Map.of("env-1/credential-1", List.of(proxy("p1", "Support bot")))));

        assertThat(collector().of("env-1", "credential-1"))
            .extracting("module", "type", "id", "name")
            .containsExactly(tuple("aim", "LLM_PROXY", "p1", "Support bot"));
    }

    @Test
    void should_gather_the_consumers_of_every_module() {
        loadModule("aim.credentialConsumers", new FakePort("aim", Map.of("env-1/credential-1", List.of(proxy("p1", "Support bot")))));
        loadModule("authz.credentialConsumers", new FakePort("authz", Map.of("env-1/credential-1", List.of(proxy("p2", "Policy sync")))));

        assertThat(collector().of("env-1", "credential-1"))
            .extracting("module", "id")
            .containsExactlyInAnyOrder(tuple("aim", "p1"), tuple("authz", "p2"));
    }

    @Test
    void should_ask_a_port_once_when_it_is_visible_under_two_names() {
        var port = new FakePort("aim", Map.of("env-1/credential-1", List.of(proxy("p1", "Support bot"))));
        loadModule("aim.credentialConsumers", port);
        loadModule("credentialConsumers", port);

        assertThat(collector().of("env-1", "credential-1")).hasSize(1);
    }
}
