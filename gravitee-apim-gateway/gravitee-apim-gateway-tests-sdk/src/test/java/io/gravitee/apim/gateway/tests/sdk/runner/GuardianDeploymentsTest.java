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
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDeploymentsTest {

    private final GuardianManager guardianManager = mock(GuardianManager.class);
    private final GuardianDeployments cut = new GuardianDeployments(new AbstractGatewayTest() {}, () -> guardianManager);

    @Test
    void should_fail_when_the_manager_does_not_deploy_the_guardian() {
        when(guardianManager.register(any())).thenReturn(false);

        assertThatThrownBy(() -> cut.deployForClass(Guardian.builder().id("guardian-id").build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("guardian-id")
            .hasMessageContaining("license");
    }
}
