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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.exchange.api.configuration.IdentifyConfiguration;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.node.container.spring.SpringEnvironmentConfiguration;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class IntegrationControllerInitializerTest {

    @Mock
    ExchangeController exchangeController;

    MockEnvironment environment = new MockEnvironment();

    IntegrationControllerInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new IntegrationControllerInitializer(
            Optional.of(exchangeController),
            Optional.of(new IdentifyConfiguration(environment, "integration"))
        );
    }

    @Test
    @SneakyThrows
    void should_start_integration_controller() {
        environment.setProperty("integration.enabled", "true");
        initializer.initialize();

        verify(exchangeController).start();
    }

    @Test
    @SneakyThrows
    void should_not_start_integration_controller_by_default() {
        initializer.initialize();

        verify(exchangeController, never()).start();
    }

    @Test
    @SneakyThrows
    void should_not_start_integration_controller_if_disabled() {
        environment.setProperty("integration.enabled", "false");
        initializer.initialize();

        verify(exchangeController, never()).start();
    }

    @Test
    @SneakyThrows
    void should_throw_when_integration_controller_starting_fails() {
        environment.setProperty("integration.enabled", "true");
        when(exchangeController.start()).thenThrow(new Exception("error"));

        var throwable = Assertions.catchThrowable(() -> initializer.initialize());

        assertThat(throwable).hasRootCauseMessage("error");
    }

    @Test
    void order_check() {
        assertThat(initializer.getOrder()).isGreaterThanOrEqualTo(100);
    }
}
