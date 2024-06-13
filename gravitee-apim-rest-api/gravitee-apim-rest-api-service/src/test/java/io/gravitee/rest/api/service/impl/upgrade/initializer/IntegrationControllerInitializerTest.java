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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.exchange.api.controller.ExchangeController;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationControllerInitializerTest {

    @Mock
    ExchangeController exchangeController;

    IntegrationControllerInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new IntegrationControllerInitializer(exchangeController);
    }

    @Test
    @SneakyThrows
    void should_start_integration_controller() {
        initializer.initialize();

        verify(exchangeController).start();
    }

    @Test
    @SneakyThrows
    void should_throw_when_integration_controller_starting_fails() {
        when(exchangeController.start()).thenThrow(new Exception("error"));

        var throwable = Assertions.catchThrowable(() -> initializer.initialize());

        assertThat(throwable).hasRootCauseMessage("error");
    }

    @Test
    void order_check() {
        assertThat(initializer.getOrder()).isGreaterThanOrEqualTo(100);
    }
}
