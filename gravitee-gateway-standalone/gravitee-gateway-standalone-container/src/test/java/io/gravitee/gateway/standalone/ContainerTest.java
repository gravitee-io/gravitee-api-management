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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.standalone.resource.ApiExternalResource;
import io.gravitee.gateway.standalone.servlet.ApiServlet;
import io.gravitee.gateway.standalone.utils.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ContainerTest {

    @ClassRule
    public static final ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/team", null);

    private static Container node;

    @BeforeClass
    public static void setUp() throws Exception {
        URL home = ContainerTest.class.getResource("/gravitee-01/");
        System.setProperty("gravitee.home", URLDecoder.decode(home.getPath(), "UTF-8"));

        node = new Container();
        node.start();
    }

    @AfterClass
    public static void stop() throws Exception {
        if (node != null) {
            node.stop();
        }
    }

    @Test
    public void call_no_api() throws IOException {
        Request request = Request.Get("http://localhost:8082/unknow");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, returnResponse.getStatusLine().getStatusCode());
    }

    private Api getApiDefinition() throws IOException {
        URL jsonFile = ContainerTest.class.getResource("/io/gravitee/gateway/standalone/api.json");
        return new GraviteeMapper().readValue(jsonFile, Api.class);
    }

    @Test
    public void call_get_started_api() throws Exception {
        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        Request request = Request.Get("http://localhost:8082/test/my_team");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void call_get_not_started_api() throws Exception {
        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();
        api.setName("not_started_api");
        api.getProxy().setContextPath("/not_started_api");
        api.setEnabled(false);

        apiManager.deploy(api);

        Request request = Request.Get("http://localhost:8082/not_started_api");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void call_get_query_params() throws Exception {
        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        String query = "query=true";
        Request request = Request.Get("http://localhost:8082/test/my_team?" + query);
        Response response = request.execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(query, responseContent);
    }

    @Test
    @Ignore
    public void call_post_content() throws Exception {
        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("case1/request_content.json");
        String content = StringUtils.copy(is);

        Request request = Request.Post("http://localhost:8082/test/my_team")
                .bodyString(content, ContentType.APPLICATION_JSON);

        Response response = request.execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(content, responseContent);
    }

    @Test
    public void call_case1_raw() throws Exception {
        String testCase = "case1";

        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(testCase + "/request_content.json");
        String content = StringUtils.copy(is);

        Request request = Request.Post("http://localhost:8082/test/my_team?case=" +  testCase)
                .bodyString(content, ContentType.APPLICATION_JSON);

        Response response = request.execute();

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(content, responseContent);
    }

    @Test
    public void call_case1_chunked_request() throws Exception {
        String testCase = "case1";

        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(testCase + "/request_content.json");
        Request request = Request.Post("http://localhost:8082/test/my_team?case=" +  testCase)
                .bodyStream(is, ContentType.APPLICATION_JSON);

        try {
            Response response = request.execute();

            HttpResponse returnResponse = response.returnResponse();
            assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

            // Not a chunked response because body content is to small
            assertEquals(null, returnResponse.getFirstHeader(HttpHeaders.TRANSFER_ENCODING));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void call_case2_chunked() throws Exception {
        String testCase = "case2";

        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(testCase + "/request_content.json");
        Request request = Request.Post("http://localhost:8082/test/my_team?case=" +  testCase)
                .bodyStream(is, ContentType.APPLICATION_JSON);

        try {
            Response response = request.execute();

            HttpResponse returnResponse = response.returnResponse();
            assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
            assertEquals("chunked", returnResponse.getFirstHeader(HttpHeaders.TRANSFER_ENCODING).getValue());

            String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
            assertEquals(652051, responseContent.length());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void call_case3_raw() throws Exception {
        String testCase = "case3";

        ApiManager apiManager = node.getApplicationContext().getBean(ApiManager.class);
        Api api = getApiDefinition();

        apiManager.deploy(api);

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(testCase + "/request_content.json");
        Request request = Request.Post("http://localhost:8082/test/my_team?mode=chunk&case=" +  testCase)
                .bodyStream(is, ContentType.APPLICATION_JSON);

        try {
            Response response = request.execute();

            HttpResponse returnResponse = response.returnResponse();
            assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

            // Set chunk mode in request but returns raw because of the size of the content
            assertEquals(null, returnResponse.getFirstHeader(HttpHeaders.TRANSFER_ENCODING));

            String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
            assertEquals(70, responseContent.length());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}
