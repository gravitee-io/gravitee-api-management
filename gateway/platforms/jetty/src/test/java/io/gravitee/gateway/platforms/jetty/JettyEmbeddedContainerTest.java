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
package io.gravitee.gateway.platforms.jetty;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.core.PlatformContext;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.core.registry.FileRegistry;
import io.gravitee.gateway.platforms.jetty.context.JettyPlatformContext;
import io.gravitee.gateway.platforms.jetty.resource.ApiExternalResource;
import io.gravitee.gateway.platforms.jetty.servlet.ApiServlet;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyEmbeddedContainerTest {

    @ClassRule
    public static ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    private JettyEmbeddedContainer container;

    @Before
    public void setUp() throws Exception {
        PlatformContext platformContext = prepareContext();
        container = new JettyEmbeddedContainer();
        container.start();
    }

    private PlatformContext prepareContext() {
        URL url = JettyEmbeddedContainerTest.class.getResource("/conf/test01");

        Registry registry = new FileRegistry(url.getPath());
        DefaultReactor reactor = new DefaultReactor();
        reactor.setRegistry(registry);

        return new JettyPlatformContext(reactor);
    }

    @After
    public void stop() throws Exception {
        container.stop();
    }

    @Test
    public void doHttpGet() throws IOException {
        Request request = Request.Get("http://localhost:8082/test");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        //assertEquals(HttpStatus.SC_BAD_REQUEST, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void doHttp404() throws IOException {
        Request request = Request.Get("http://localhost:8082/unknow");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, returnResponse.getStatusLine().getStatusCode());
    }
}
