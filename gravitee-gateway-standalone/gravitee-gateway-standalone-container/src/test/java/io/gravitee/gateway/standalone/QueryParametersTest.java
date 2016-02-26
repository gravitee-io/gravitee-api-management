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
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import io.gravitee.gateway.standalone.utils.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

import java.net.URLEncoder;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team"
)
public class QueryParametersTest extends AbstractGatewayTest {

    @Test
    public void call_get_query_params() throws Exception {
        String query = "query=true";
        Request request = Request.Get("http://localhost:8082/test/my_team?" + query);
        Response response = request.execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_params_emptyvalue() throws Exception {
        String query = "query";
        Request request = Request.Get("http://localhost:8082/test/my_team?" + query);
        Response response = request.execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_params_spaces() throws Exception {
        String query = "q=myparam:test+AND+myotherparam:12";
        String encodedQuery = URLEncoder.encode(query, "UTF-8");

        Request request = Request.Get("http://localhost:8082/test/my_team?" + encodedQuery);
        Response response = request.execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }
}
