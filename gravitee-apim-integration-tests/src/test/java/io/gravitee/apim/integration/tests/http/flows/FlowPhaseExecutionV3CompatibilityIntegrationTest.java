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
package io.gravitee.apim.integration.tests.http.flows;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowPhaseExecutionV3CompatibilityIntegrationTest extends FlowPhaseExecutionV3IntegrationTest {

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DisplayName("Flows without condition and operator 'STARTS_WITH'")
    class NoConditionOperatorStartsWith extends FlowPhaseExecutionV3IntegrationTest.NoConditionOperatorStartsWith {}

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DisplayName("Flows without condition and operator 'EQUALS'")
    class NoConditionOperatorEquals extends FlowPhaseExecutionV3IntegrationTest.NoConditionOperatorEquals {}

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DisplayName("Flows without condition and mixed operators")
    class NoConditionOperatorMixed extends FlowPhaseExecutionV3IntegrationTest.NoConditionOperatorMixed {}

    @Nested
    @GatewayTest(mode = GatewayMode.COMPATIBILITY)
    @DisplayName("Flows without condition and mixed operators")
    class ConditionalFlows extends FlowPhaseExecutionV3IntegrationTest.ConditionalFlows {}
}
