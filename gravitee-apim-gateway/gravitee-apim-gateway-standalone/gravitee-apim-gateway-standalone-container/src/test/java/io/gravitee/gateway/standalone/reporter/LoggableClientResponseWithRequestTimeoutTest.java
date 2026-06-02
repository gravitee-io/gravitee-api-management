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
package io.gravitee.gateway.standalone.reporter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.vertx.rxjava3.RxHelper;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor(value = "/io/gravitee/gateway/standalone/http/teams.json", configFolder = "/gravitee-requestTimeout")
public class LoggableClientResponseWithRequestTimeoutTest extends AbstractWiremockGatewayTest {

    private FakeReporter fakeReporter;

    @BeforeEach
    public void setup() {
        fakeReporter = (FakeReporter) context.getBean("fakeReporter");
    }

    @AfterEach
    public void tearDown() {
        fakeReporter.reset();
    }

    @Test
    public void reporter_should_be_called_twice_because_loggin_enabled() throws Exception {
        final CountDownLatch latchMetric = new CountDownLatch(1);
        final CountDownLatch latchLog = new CountDownLatch(1);

        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Metrics) {
                latchMetric.countDown();
            } else if (reportable instanceof Log) {
                latchLog.countDown();
            }
        });

        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")));

        assertTrue(latchMetric.await(2, TimeUnit.SECONDS), "Reporter should have been called for Metric reportable");
        assertTrue(latchLog.await(100, TimeUnit.MILLISECONDS), "Reporter should have been called for Log reportable");
    }
}
