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
import io.gravitee.gateway.api.Node;
import io.gravitee.gateway.platforms.jetty.resource.ApiExternalResource;
import io.gravitee.gateway.platforms.jetty.servlet.ApiServlet;
import io.gravitee.gateway.platforms.jetty.spring.JettyConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public class JettyEmbeddedContainerTest {

    @Configuration
    @Import({JettyConfiguration.class})
    static class ContextConfiguration {
    }

    @ClassRule
    public static ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    @Autowired
    private Node node;

    @Before
    public void setUp() throws Exception {
        node.start();
    }

    @After
    public void stop() throws Exception {
        node.stop();
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
