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
package io.gravitee.apim.integration.tests.http.failover;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class FailoverV4EmulationIntegrationTest extends FailoverV3IntegrationTest {

    @Nested
    @GatewayTest
    class CircuitBreakingCases extends FailoverV3IntegrationTest.CircuitBreakingCases {}

    @Nested
    @GatewayTest
    class OnlyOneEndpointInGroup extends FailoverV3IntegrationTest.OnlyOneEndpointInGroup {}

    @Nested
    @GatewayTest
    class MultipleEndpointsInGroup extends FailoverV3IntegrationTest.MultipleEndpointsInGroup {}

    @Nested
    @GatewayTest
    class DynamicRoutingToEndpoint extends FailoverV3IntegrationTest.DynamicRoutingToEndpoint {}

    @Nested
    @GatewayTest
    class DynamicRoutingToGroup extends FailoverV3IntegrationTest.DynamicRoutingToGroup {}
}
