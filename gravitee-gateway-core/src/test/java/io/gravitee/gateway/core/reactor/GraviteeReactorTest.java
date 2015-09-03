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
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.AbstractCoreTest;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.external.ApiExternalResource;
import io.gravitee.gateway.core.external.ApiServlet;
import io.gravitee.gateway.core.http.ServerRequest;
import io.gravitee.gateway.core.http.ServerResponse;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.plugin.Plugin;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.reporter.ConsoleReporter;
import io.gravitee.gateway.core.reporter.ReporterManager;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.net.URI;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorTest extends AbstractCoreTest {

    @ClassRule
    public static final ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    @Autowired
    private GraviteeReactor<Observable<Response>> reactor;

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
    public void processRequest_startedApi() {
        // Register API endpoint
        eventManager.publishEvent(ApiEvent.CREATE, new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .start()
                .build());

        ServerRequest req = new ServerRequest();
        ServerResponse response = new ServerResponse();

        req.setRequestURI(URI.create("http://localhost/team"));
        req.setMethod(HttpMethod.GET);

        Response resp = reactor.process(req, response).toBlocking().single();
        Assert.assertEquals(HttpStatusCode.OK_200, resp.status());
    }

    @Test
    public void processRequest_notYetStartedApi() {
        // Register API endpoint
        eventManager.publishEvent(ApiEvent.CREATE, new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost:8083/myapi")
                .build());

        ServerRequest req = new ServerRequest();
        ServerResponse response = new ServerResponse();

        req.setRequestURI(URI.create("http://localhost/team"));
        req.setMethod(HttpMethod.GET);

        Response resp = reactor.process(req, response).toBlocking().single();
        Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status());
    }

    @Test
    public void processNotFoundRequest() {
        // Register API endpoint
        eventManager.publishEvent(ApiEvent.CREATE, new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost/myapi")
                .build());

        ServerRequest req = new ServerRequest();
        req.setRequestURI(URI.create("http://localhost/unknown_path"));
        req.setMethod(HttpMethod.GET);

        ServerResponse response = new ServerResponse();

        Response resp = reactor.process(req, response).toBlocking().single();
        Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status());
    }

    @Test
    public void reporter_checkReport() {
        ((PluginHandler) reporterManager).handle(new Plugin() {
            @Override
            public String id() {
                return "console-reporter";
            }

            @Override
            public Class<?> clazz() {
                return ConsoleReporter.class;
            }
        });

        Assert.assertEquals(1, reporterManager.getReporters().size());

//        Reporter reporter = spy(reporterManager.getReporters().iterator().next());

        // Register API endpoint
        eventManager.publishEvent(ApiEvent.CREATE, new ApiBuilder()
                .name("my-team-api")
                .origin("http://localhost/team")
                .target("http://localhost/myapi")
                .build());

        ServerRequest req = new ServerRequest();
        req.setRequestURI(URI.create("http://localhost/unknown_path"));
        req.setMethod(HttpMethod.GET);

        ServerResponse response = new ServerResponse();

        reactor.process(req, response).toBlocking().single();

        // check that the reporter has been correctly called
//        verify(reporter, atLeastOnce()).report(eq(req), any(Response.class));
    }
}
