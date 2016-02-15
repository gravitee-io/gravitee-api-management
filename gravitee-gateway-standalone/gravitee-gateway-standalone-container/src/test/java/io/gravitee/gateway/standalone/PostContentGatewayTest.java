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

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import io.gravitee.gateway.standalone.utils.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team"
)
public class PostContentGatewayTest extends AbstractGatewayTest {

    @Test
    public void call_post_content() throws Exception {
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

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(testCase + "/request_content.json");
        Request request = Request.Post("http://localhost:8082/test/my_team?case=" +  testCase)
                .bodyStream(is, ContentType.APPLICATION_JSON);

        try {
            Response response = request.execute();

            HttpResponse returnResponse = response.returnResponse();
            assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
            assertEquals(HttpHeadersValues.TRANSFER_ENCODING_CHUNKED, returnResponse.getFirstHeader(HttpHeaders.TRANSFER_ENCODING).getValue());

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
