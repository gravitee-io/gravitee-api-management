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
package testcases;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayMode;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

@EnableForGatewayTestingExtensionTesting
public class GatewayModeTestCase {

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/nothing.json" })
    class V4EmulationEngine extends AbstractGatewayTest {

        @Test
        void should_set_the_api_execution_mode_for_api_deployed_on_class() {
            var a = getDeployedForTestClass().get("my-api");
            final Api api = (Api) a.getDefinition();
            assertThat(api.getExecutionMode()).isEqualTo(ExecutionMode.V4_EMULATION_ENGINE);
        }

        @Test
        @DeployApi({ "/apis/teams.json" })
        void should_set_the_api_execution_mode_for_api_deployed_on_method() {
            var a = deployedApis.get("api-test");
            final Api api = (Api) a.getDefinition();
            assertThat(api.getExecutionMode()).isEqualTo(ExecutionMode.V4_EMULATION_ENGINE);
        }

        @Test
        void should_enable_emulate_engine() {
            var env = getBean(Environment.class);
            assertThat(env)
                .isNotNull()
                .extracting(e -> e.getProperty("api.v2.emulateV4Engine.default"))
                .isEqualTo("yes");
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi({ "/apis/nothing.json" })
    class V3Engine extends AbstractGatewayTest {

        @Test
        void should_set_the_api_execution_mode_for_api_deployed_on_class() {
            var a = getDeployedForTestClass().get("my-api");
            final Api api = (Api) a.getDefinition();
            assertThat(api.getExecutionMode()).isEqualTo(ExecutionMode.V3);
        }

        @Test
        @DeployApi({ "/apis/teams.json" })
        void should_set_the_api_execution_mode_for_api_deployed_on_method() {
            var a = deployedApis.get("api-test");
            final Api api = (Api) a.getDefinition();
            assertThat(api.getExecutionMode()).isEqualTo(ExecutionMode.V3);
        }

        @Test
        void should_disable_emulate_engine() {
            var env = getBean(Environment.class);
            assertThat(env)
                .isNotNull()
                .extracting(e -> e.getProperty("api.v2.emulateV4Engine.default"))
                .isEqualTo("no");
        }
    }
}
