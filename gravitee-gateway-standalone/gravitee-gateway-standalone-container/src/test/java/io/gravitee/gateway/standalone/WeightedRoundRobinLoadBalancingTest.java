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

import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.servlet.EchoServlet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/echo-lb-weight.json")
@ApiConfiguration(
        servlet = EchoServlet.class,
        contextPath = "/echo",
        workers = 2
)
public class WeightedRoundRobinLoadBalancingTest extends AbstractGatewayTest {

    @Test
    public void call_weighted_lb_multiple_endpoints() throws Exception {
        Request request = Request.Get("http://localhost:8082/echo/helloworld");
        Map<String, Integer> counts = new HashMap<>();
        int calls = 10;

        for(int i = 0 ; i < calls ; i++) {
            Response response = request.execute();
            HttpResponse returnResponse = response.returnResponse();

            assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

            String workerHeader = returnResponse.getFirstHeader("worker").getValue();

            Integer workerCount = counts.get(workerHeader);
            if (workerCount == null) {
                workerCount = 1;
            } else {
                ++workerCount;
            }
            counts.put(workerHeader, workerCount);

        }

        assertEquals(3, (int) counts.get("worker#0"));
        assertEquals(7, (int) counts.get("worker#1"));
    }
}
