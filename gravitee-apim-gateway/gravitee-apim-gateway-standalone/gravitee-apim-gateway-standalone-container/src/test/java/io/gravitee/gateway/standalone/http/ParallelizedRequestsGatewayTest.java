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
package io.gravitee.gateway.standalone.http;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import org.junit.Test;

import java.util.concurrent.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class ParallelizedRequestsGatewayTest extends AbstractWiremockGatewayTest {

    private final int NUMBER_OF_CLIENTS = 10;
    private final int NUMBER_OF_REQUESTS = 1000;

    private final ExecutorService executorService =
            new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>());

    @Test
    public void shouldProcessParallelRequests() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        Vertx vertx = Vertx.vertx();

        HttpClient client = vertx.createHttpClient();
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_CLIENTS * NUMBER_OF_REQUESTS);

        Runnable calls = () -> {
            for (int i = 0 ; i < NUMBER_OF_REQUESTS ; i++) {
                client.requestAbs(HttpMethod.GET, "http://localhost:8082/test/my_team", response -> {
                    assertEquals(HttpStatusCode.OK_200, response.statusCode());
                    latch.countDown();
                }).end();
            }
        };

        for (int i = 0 ; i < NUMBER_OF_CLIENTS ; i++) {
            executorService.submit(calls);
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        wireMockRule.verify(NUMBER_OF_CLIENTS * NUMBER_OF_REQUESTS,
                getRequestedFor(urlPathEqualTo("/team/my_team")));
    }
}
