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
package io.gravitee.apim.infra.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.apim.core.credential.service_provider.CredentialConsumerPort;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumerPort.Change;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumerPort.Consumer;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumerPort.Warning;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Wires the collector the way the plugin contexts are wired: a loaded module's beans are copied into the APIM context
 * under a {@code <module>.<bean>} name, after that context has started.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ModuleCredentialConsumersTest {

    private GenericApplicationContext apim;

    record FakePort(String module, Map<String, List<Consumer>> byCredential) implements CredentialConsumerPort {
        @Override
        public List<Consumer> consumersOf(String environmentId, String credentialId) {
            return byCredential.getOrDefault(environmentId + "/" + credentialId, List.of());
        }
    }

    static class ListeningPort implements CredentialConsumerPort {

        private final List<Change> received = new ArrayList<>();
        private final String module;
        private final boolean failing;
        private final List<Warning> warnings;

        ListeningPort(boolean failing) {
            this("aim", failing, List.of());
        }

        ListeningPort(String module, boolean failing, List<Warning> warnings) {
            this.module = module;
            this.failing = failing;
            this.warnings = warnings;
        }

        @Override
        public String module() {
            return module;
        }

        @Override
        public List<Consumer> consumersOf(String environmentId, String credentialId) {
            return List.of();
        }

        @Override
        public List<Warning> changed(Change change) {
            if (failing) {
                throw new IllegalStateException("module down");
            }
            received.add(change);
            return warnings;
        }
    }

    private static final Change CHANGE = new Change("org-1", "env-1", "credential-1", "user-1");

    @BeforeEach
    void setUp() {
        apim = new GenericApplicationContext();
        apim.registerBean(ModuleCredentialConsumers.class, () ->
            new ModuleCredentialConsumers(apim.getBeanProvider(CredentialConsumerPort.class))
        );
        apim.refresh();
    }

    @AfterEach
    void tearDown() {
        apim.close();
    }

    private CredentialConsumers collector() {
        return apim.getBean(CredentialConsumers.class);
    }

    private void loadModule(String beanName, CredentialConsumerPort port) {
        apim.getBeanFactory().registerSingleton(beanName, port);
    }

    private static Consumer proxy(String id, String name) {
        return new Consumer("LLM_PROXY", id, name);
    }

    @Test
    void should_find_no_consumer_when_no_module_reports_any() {
        assertThat(collector().of("env-1", "credential-1")).isEmpty();
    }

    @Test
    void should_ask_a_module_loaded_after_the_context_started() {
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

    @Test
    void should_tell_every_module_of_a_change() {
        var aim = new ListeningPort(false);
        var authz = new ListeningPort(false);
        loadModule("aim.credentialConsumers", aim);
        loadModule("authz.credentialConsumers", authz);

        collector().changed(CHANGE);

        assertThat(aim.received).containsExactly(CHANGE);
        assertThat(authz.received).containsExactly(CHANGE);
    }

    @Test
    void should_tell_a_port_once_when_it_is_visible_under_two_names() {
        var port = new ListeningPort(false);
        loadModule("aim.credentialConsumers", port);
        loadModule("credentialConsumers", port);

        collector().changed(CHANGE);

        assertThat(port.received).containsExactly(CHANGE);
    }

    @Test
    void should_still_tell_the_other_modules_when_one_fails() {
        var other = new ListeningPort(false);
        loadModule("aim.credentialConsumers", new ListeningPort(true));
        loadModule("authz.credentialConsumers", other);

        collector().changed(CHANGE);

        assertThat(other.received).containsExactly(CHANGE);
    }

    @Test
    void should_gather_the_warnings_of_every_module() {
        loadModule(
            "aim.credentialConsumers",
            new ListeningPort("aim", false, List.of(new Warning("llm-proxy", "p1", "Support bot", "You cannot update this LLM proxy.")))
        );
        loadModule(
            "authz.credentialConsumers",
            new ListeningPort("authz", false, List.of(new Warning("policy-sync", "p2", "Policy sync", "The sync is paused.")))
        );

        assertThat(collector().changed(CHANGE))
            .extracting("module", "type", "id", "name", "message")
            .containsExactlyInAnyOrder(
                tuple("aim", "llm-proxy", "p1", "Support bot", "You cannot update this LLM proxy."),
                tuple("authz", "policy-sync", "p2", "Policy sync", "The sync is paused.")
            );
    }

    @Test
    void should_keep_the_warnings_of_the_other_modules_when_one_fails() {
        loadModule("aim.credentialConsumers", new ListeningPort(true));
        loadModule(
            "authz.credentialConsumers",
            new ListeningPort("authz", false, List.of(new Warning("policy-sync", "p2", "Policy sync", "The sync is paused.")))
        );

        assertThat(collector().changed(CHANGE)).extracting("module", "id").containsExactly(tuple("authz", "p2"));
    }

    @Test
    void should_keep_the_warnings_of_the_other_modules_when_one_returns_a_null_warning() {
        loadModule("aim.credentialConsumers", new ListeningPort("aim", false, Arrays.asList((Warning) null)));
        loadModule(
            "authz.credentialConsumers",
            new ListeningPort("authz", false, List.of(new Warning("policy-sync", "p2", "Policy sync", "The sync is paused.")))
        );

        assertThat(collector().changed(CHANGE)).extracting("module", "id").containsExactly(tuple("authz", "p2"));
    }
}
