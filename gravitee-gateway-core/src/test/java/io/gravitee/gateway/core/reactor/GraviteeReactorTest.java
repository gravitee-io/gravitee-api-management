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
package io.gravitee.gateway.core.reactor;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.AbstractCoreTest;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.gateway.core.external.ApiExternalResource;
import io.gravitee.gateway.core.external.ApiServlet;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorTest extends AbstractCoreTest {

    @ClassRule
    public static final ApiExternalResource SERVER_MOCK = new ApiExternalResource(ApiServlet.class, "/*", null);

    @Autowired
    private GraviteeReactor reactor;

    @Autowired
    private EventManager eventManager;

    @Before
    public void setUp() throws Exception {
        reactor.doStart();
    }

    @After
    public void tearDown() throws Exception {
        reactor.clearHandlers();
        reactor.doStop();
    }

    @Test
    public void processRequest_startedApi() throws Exception {
        // Register API endpoint
        Api api = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, api);

        HttpServerRequest req = new HttpServerRequest();
        req.setMethod(HttpMethod.GET);
        req.setRequestURI(URI.create("http://localhost/team"));

        Response response = new HttpServerResponse();

        final CountDownLatch lock = new CountDownLatch(1);
        reactor.process(req, response,
                resp -> {
                    Assert.assertEquals(HttpStatusCode.OK_200, resp.status());
                    lock.countDown();
                });

        req.endHandler().handle(null);
        Assert.assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void processRequest_startedApi_unknownPath() throws Exception {
        // Register API endpoint
        Api api = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, api);

        HttpServerRequest req = new HttpServerRequest();
        req.setMethod(HttpMethod.GET);
        req.setRequestURI(URI.create("http://localhost/teams"));

        Response response = new HttpServerResponse();

        final CountDownLatch lock = new CountDownLatch(1);
        reactor.process(req, response,
                resp -> {
                    Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status());
                    lock.countDown();
                });

        Assert.assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void processRequest_startedApi_gatewayError() throws Exception {
        // Register API endpoint
        Api api = getUnreachableApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, api);

        HttpServerRequest req = new HttpServerRequest();
        req.setMethod(HttpMethod.GET);
        req.setRequestURI(URI.create("http://localhost/team"));

        Response response = new HttpServerResponse();

        final CountDownLatch lock = new CountDownLatch(1);
        reactor.process(req, response,
                resp -> {
                    Assert.assertEquals(HttpStatusCode.BAD_GATEWAY_502, resp.status());
                    lock.countDown();
                });

        req.endHandler().handle(null);
        Assert.assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void processRequest_notYetStartedApi() throws Exception {
        // Register API endpoint
        Api api = getApiDefinition();
        api.setEnabled(false);

        eventManager.publishEvent(ApiEvent.DEPLOY, api);

        HttpServerRequest req = new HttpServerRequest();
        req.setMethod(HttpMethod.GET);
        req.setRequestURI(URI.create("http://localhost/team"));

        Response response = new HttpServerResponse();

        final CountDownLatch lock = new CountDownLatch(1);
        reactor.process(req, response,
                resp -> {
                    Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status());
                    lock.countDown();
                });

        Assert.assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void processNotFoundRequest() throws Exception {
        // Register API endpoint
        Api api = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, api);

        HttpServerRequest req = new HttpServerRequest();
        req.setMethod(HttpMethod.GET);
        req.setRequestURI(URI.create("http://localhost/unknown_path"));

        Response response = new HttpServerResponse();

        final CountDownLatch lock = new CountDownLatch(1);
        reactor.process(req, response,
                resp -> {
                    Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status());
                    lock.countDown();
                });

        Assert.assertEquals(true, lock.await(1000, TimeUnit.MILLISECONDS));
    }

    private Api getApiDefinition() throws Exception {
        URL jsonFile = GraviteeReactorTest.class.getResource("/io/gravitee/gateway/core/reactor/api.json");
        Api api = new GraviteeMapper().readValue(jsonFile, Api.class);
        String endpoint = api.getProxy().getEndpoints().iterator().next().getTarget();
        URI uri = URI.create(endpoint);
        URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), SERVER_MOCK.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        api.getProxy().getEndpoints().clear();
        api.getProxy().getEndpoints().add(new Endpoint(newUri.toString()));
        return api;
    }

    private Api getUnreachableApiDefinition() throws IOException {
        URL jsonFile = GraviteeReactorTest.class.getResource("/io/gravitee/gateway/core/reactor/api-unreachable.json");
        return new GraviteeMapper().readValue(jsonFile, Api.class);
    }
}
