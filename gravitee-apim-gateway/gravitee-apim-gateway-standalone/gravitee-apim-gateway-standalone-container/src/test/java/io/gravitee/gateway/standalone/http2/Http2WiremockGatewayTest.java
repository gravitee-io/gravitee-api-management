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
package io.gravitee.gateway.standalone.http2;

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import org.apache.http.client.fluent.Executor;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.Before;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class Http2WiremockGatewayTest extends AbstractWiremockGatewayTest {
    static {
        System.setProperty("http.secured", "true");
        System.setProperty("http.alpn", "true");
        System.setProperty("http.ssl.keystore.type", "self-signed");
    }

    @Before
    public void initExecutor() throws Exception {
        // Create a dedicated HttpClient for each test with no pooling to avoid side effects.
        final SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
        final CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        this.executor = Executor.newInstance(client);
    }
}
