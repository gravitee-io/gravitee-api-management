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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.utils.StringUtils;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class QueryParametersTest extends AbstractWiremockGatewayTest {

    @Test
    public void call_get_query_params() throws Exception {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team")).willReturn(ok().withBody("{{request.query.q}}").withTransformers("response-template"))
        );

        String query = "true";
        URI target = new URIBuilder("http://localhost:8082/test/my_team").addParameter("q", "true").build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals(query, responseContent);

        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_query_params_emptyvalue() throws Exception {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team")).willReturn(ok().withBody("{{request.query.q}}").withTransformers("response-template"))
        );

        URI target = new URIBuilder("http://localhost:8082/test/my_team").addParameter("q", null).build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals("", responseContent);

        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_query_params_spaces() throws Exception {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team")).willReturn(ok().withBody("{{request.query.q}}").withTransformers("response-template"))
        );

        String query = "myparam:test+AND+myotherparam:12";
        URI target = new URIBuilder("http://localhost:8082/test/my_team").addParameter("q", query).build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals(query, responseContent);

        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_query_accent() throws Exception {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team")).willReturn(ok().withBody("{{request.query.q}}").withTransformers("response-template"))
        );

        String query = "poupée";

        URI target = new URIBuilder("http://localhost:8082/test/my_team").addParameter("q", query).build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals(query, responseContent);

        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_query_with_special_separator() throws Exception {
        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team")).willReturn(ok().withBody("{{request.query.q}}").withTransformers("response-template"))
        );

        String query = "from:2016-01-01;to:2016-01-31";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
            .addParameter("id", "20000047")
            .addParameter("idType", "1")
            .addParameter("q", query)
            .build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals(query, responseContent);

        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_query_with_json_content() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok()));

        String query = "{\"key\": \"value\"}";

        URI target = new URIBuilder("http://localhost:8082/test/my_team").addParameter(query, null).build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        QueryStringEncoder encoder = new QueryStringEncoder("/team/my_team");
        encoder.addParam(query, null);

        wireMockRule.verify(getRequestedFor(urlEqualTo("/team/my_team?" + target.getRawQuery())));
    }

    @Test
    public void call_get_query_with_multiple_parameter_values() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok()));

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
            .addParameter("country", "fr")
            .addParameter("country", "es")
            .addParameter("type", "MAG")
            .build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        QueryStringEncoder encoder = new QueryStringEncoder("/team/my_team");
        encoder.addParam("country", "fr");
        encoder.addParam("country", "es");
        encoder.addParam("type", "MAG");

        wireMockRule.verify(getRequestedFor(urlEqualTo(encoder.toString())));
    }

    @Test
    @Ignore
    public void call_get_query_with_multiple_parameter_values_ordered() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok()));

        String query = "country=fr&type=MAG&country=es";

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
            .addParameter("country", "fr")
            .addParameter("type", "MAG")
            .addParameter("country", "es")
            .build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        wireMockRule.verify(getRequestedFor(urlEqualTo("/team/my_team?" + query)));
    }

    @Test
    public void call_multiple_characters() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok()));
        String query = "RECHERCHE,35147,8;RECHERCHE,670620,1";

        URI target = new URIBuilder("http://localhost:8082/test/my_team").addParameter("q", "RECHERCHE,35147,8;RECHERCHE,670620,1").build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        QueryStringEncoder encoder = new QueryStringEncoder("/team/my_team");
        encoder.addParam("q", query);

        wireMockRule.verify(getRequestedFor(urlEqualTo(encoder.toString())));
    }

    @Test
    public void call_percent_character() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok()));

        URI target = new URIBuilder("http://localhost:8082/test/my_team")
            .addParameter("username", "toto")
            .addParameter("password", "password%")
            .build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        QueryStringEncoder encoder = new QueryStringEncoder("/team/my_team");
        encoder.addParam("username", "toto");
        encoder.addParam("password", "password%");

        wireMockRule.verify(getRequestedFor(urlEqualTo(encoder.toString())));
    }
}
