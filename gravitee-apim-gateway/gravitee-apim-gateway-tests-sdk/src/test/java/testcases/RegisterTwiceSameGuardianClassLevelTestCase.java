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

import static org.junit.jupiter.api.Assertions.fail;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployGuardians;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import org.junit.jupiter.api.Test;

/**
 * The Guardian has no environment: the SDK gives it the default one, which the duplicate check must take into account.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployGuardians({ "/guardians/guardian-without-environment.json", "/guardians/guardian-without-environment.json" })
@EnableForGatewayTestingExtensionTesting
public class RegisterTwiceSameGuardianClassLevelTestCase extends AbstractGatewayTest {

    @Test
    void fakeMethod() {
        fail();
    }
}
