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
import org.apache.http.client.utils.URIBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

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
        String query = "true";
        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("q", "true")
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_params_emptyvalue() throws Exception {
        String query = "";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("q", null)
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_params_spaces() throws Exception {
        String query = "myparam:test+AND+myotherparam:12";
        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("q", query)
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_accent() throws Exception {
        String query = "poupée";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("q", query)
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_with_special_separator() throws Exception {
        String query = "from:2016-01-01;to:2016-01-31";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("id", "20000047")
                .addParameter("idType", "1")
                .addParameter("q", query)
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_with_json_content() throws Exception {
        String query = "{\"key\": \"value\"}";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .setQuery(query)
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_get_query_with_multiple_parameter_values() throws Exception {
        String query = "country=fr&country=es&type=MAG";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("country", "fr")
                .addParameter("country", "es")
                .addParameter("type", "MAG")
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    @Ignore
    public void call_get_query_with_multiple_parameter_values_ordered() throws Exception {
        String query = "country=fr&type=MAG&country=es";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("country", "fr")
                .addParameter("type", "MAG")
                .addParameter("country", "es")
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_multiple_characters() throws Exception {
        String query = "RECHERCHE,35147,8;RECHERCHE,670620,1";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("q", "RECHERCHE,35147,8;RECHERCHE,670620,1")
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    public void call_percent_character() throws Exception {
        String query = "username=toto&password=password%";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
                .addParameter("username", "toto")
                .addParameter("password", "password%")
                .build();

        Response response = Request.Get(target).execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }
}
