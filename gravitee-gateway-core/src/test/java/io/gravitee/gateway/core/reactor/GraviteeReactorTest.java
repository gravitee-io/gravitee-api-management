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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.core.AbstractCoreTest;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.external.ApiExternalResource;
import io.gravitee.gateway.core.external.ApiServlet;
import io.gravitee.gateway.core.http.HttpServerRequest;
import io.gravitee.gateway.core.http.HttpServerResponse;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.reporter.ConsoleReporter;
import io.gravitee.gateway.core.reporter.ReporterManager;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginManifest;
import io.gravitee.plugin.api.PluginType;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorTest extends AbstractCoreTest {

    @ClassRule
    public static final ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    @Autowired
    private GraviteeReactor reactor;

    @Autowired
    private ReporterManager reporterManager;

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
    public void processRequest_startedApi() throws IOException {
        // Register API endpoint
        ApiDefinition apiDefinition = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, apiDefinition);

        HttpServerRequest req = new HttpServerRequest();
        HttpServerResponse response = new HttpServerResponse();
        response.setOutputStream(new ByteArrayOutputStream());

        req.setRequestURI(URI.create("http://localhost/team"));
        req.setMethod(HttpMethod.GET);

        reactor.process(req, response,
                resp -> Assert.assertEquals(HttpStatusCode.OK_200, resp.status()));
    }

    @Test
    public void processRequest_startedApi_gatewayError() throws IOException {
        // Register API endpoint
        ApiDefinition apiDefinition = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, apiDefinition);

        HttpServerRequest req = new HttpServerRequest();
        HttpServerResponse response = new HttpServerResponse();

        req.setRequestURI(URI.create("http://localhost/team"));
        req.setMethod(HttpMethod.GET);

        reactor.process(req, response,
                resp -> Assert.assertEquals(HttpStatusCode.BAD_GATEWAY_502, resp.status()));
    }

    @Test
    public void processRequest_notYetStartedApi() throws IOException {
        // Register API endpoint
        ApiDefinition apiDefinition = getApiDefinition();
        apiDefinition.setEnabled(false);

        eventManager.publishEvent(ApiEvent.DEPLOY, apiDefinition);

        HttpServerRequest req = new HttpServerRequest();
        HttpServerResponse response = new HttpServerResponse();

        req.setRequestURI(URI.create("http://localhost/team"));
        req.setMethod(HttpMethod.GET);

        reactor.process(req, response,
                resp -> Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status()));
    }

    @Test
    public void processNotFoundRequest() throws IOException {
        // Register API endpoint
        ApiDefinition apiDefinition = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, apiDefinition);

        HttpServerRequest req = new HttpServerRequest();
        req.setRequestURI(URI.create("http://localhost/unknown_path"));
        req.setMethod(HttpMethod.GET);

        HttpServerResponse response = new HttpServerResponse();

        reactor.process(req, response,
                resp -> Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status()));
    }

    @Test
    public void reporter_checkReport() throws IOException {
        ((PluginHandler) reporterManager).handle(new Plugin() {
            @Override
            public String id() {
                return "console-reporter";
            }

            @Override
            public Class<?> clazz() {
                return ConsoleReporter.class;
            }

            @Override
            public PluginType type() {
                return null;
            }

            @Override
            public Path path() {
                return null;
            }

            @Override
            public PluginManifest manifest() {
                return null;
            }

            @Override
            public URL[] dependencies() {
                return new URL[0];
            }
        });

        Assert.assertEquals(1, reporterManager.getReporters().size());

//        Reporter reporter = spy(reporterManager.getReporters().iterator().next());

        // Register API endpoint
        ApiDefinition apiDefinition = getApiDefinition();

        eventManager.publishEvent(ApiEvent.DEPLOY, apiDefinition);

        HttpServerRequest req = new HttpServerRequest();
        req.setRequestURI(URI.create("http://localhost/unknown_path"));
        req.setMethod(HttpMethod.GET);

        HttpServerResponse response = new HttpServerResponse();

        reactor.process(req, response, result -> {});

        // check that the reporter has been correctly called
//        verify(reporter, atLeastOnce()).report(eq(req), any(Response.class));
    }

    private ApiDefinition getApiDefinition() throws IOException {
        URL jsonFile = GraviteeReactorTest.class.getResource("/io/gravitee/gateway/core/reactor/api.json");
        return new ObjectMapper().readValue(jsonFile, ApiDefinition.class);
    }
}
