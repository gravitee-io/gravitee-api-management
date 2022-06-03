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
package io.gravitee.apim.gateway.tests.sdk;

import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.reactiverse.junit5.web.WebClientOptionsInject;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractHttp2GatewayTest extends AbstractGatewayTest {

    @WebClientOptionsInject
    public WebClientOptions options = new WebClientOptions()
        .setDefaultHost("localhost")
        .setDefaultPort(gatewayPort())
        .setSsl(true)
        .setVerifyHost(false)
        .setTrustAll(true);

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set("http.secured", "true").set("http.alpn", "true").set("http.ssl.keystore.type", "self-signed");
    }
}
