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

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.handlers.api.definition.Api;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@EnableForGatewayTestingExtensionTesting
@DeployApi({ "/apis/success-flow.json" })
public class ManuallyDeployTestCase extends AbstractGatewayTest {

    @Test
    @DisplayName("Should not deploy an api that is deployed at class level")
    void shouldNotDeployApiThatIsDeployedAtClassLevel() {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        definition.setId("my-api");
        final Api api = new Api(definition);
        deploy(api);
    }

    @Test
    @DisplayName("Should not undeploy an api that is deployed at class level")
    void shouldNotUnDeployApiThatIsDeployedAtClassLevel() {
        undeploy("my-api");
    }
}
