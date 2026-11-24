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
package io.gravitee.apim.gateway.tests.sdk.runner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.gateway.handlers.guardrail.manager.GuardrailManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailDeploymentsTest {

    private final GuardrailManager guardrailManager = mock(GuardrailManager.class);
    private final GuardrailDeployments cut = new GuardrailDeployments(new AbstractGatewayTest() {}, () -> guardrailManager);

    @Test
    void should_fail_when_the_manager_does_not_deploy_the_guardrail() {
        when(guardrailManager.register(any())).thenReturn(false);

        assertThatThrownBy(() -> cut.deployForClass(Guardrail.builder().id("guardrail-id").build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("guardrail-id")
            .hasMessageContaining("license");
    }
}
