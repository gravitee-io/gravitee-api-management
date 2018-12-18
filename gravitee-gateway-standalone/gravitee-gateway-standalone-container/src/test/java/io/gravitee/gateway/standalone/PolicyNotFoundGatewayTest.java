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
package io.gravitee.gateway.standalone;

import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
public class PolicyNotFoundGatewayTest extends AbstractGatewayTest {
    
    @Test
    public void shouldReturnNotFound_unknownPolicy() throws Exception {
        HttpResponse response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();

        // The gateway returns a NOT_FOUND (404) because the API can't be deployed correctly.
        // The API is not correctly deployed because a required policy can not be found
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Override
    public void register(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        // Do not install any policy in registry
    }
}
