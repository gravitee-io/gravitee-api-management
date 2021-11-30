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
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.utils.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class PostContentGatewayTest extends AbstractWiremockGatewayTest {

    @Test
    @Ignore
    /*
      This test seems to not work anymore since latest Wiremock & Jetty upgrade.
      Content_type response header is no more sent back and replaced by transfer-encoding.
     */
    public void small_body_with_content_length() throws Exception {
        String mockContent = StringUtils.copy(getClass().getClassLoader().getResourceAsStream("case1/response_content.json"));

        stubFor(
            post(urlEqualTo("/team/my_team"))
                .willReturn(
                    ok()
                        .withBody(mockContent)
                        .withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_LENGTH, Integer.toString(mockContent.length()))
                        .withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                )
        );

        Request request = Request.Post("http://localhost:8082/test/my_team").bodyString(mockContent, ContentType.APPLICATION_JSON);
        HttpResponse response = request.execute().returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String responseContent = StringUtils.copy(response.getEntity().getContent());

        assertEquals(mockContent, responseContent);
        assertEquals(mockContent.length(), Integer.parseInt(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH).getValue()));
        assertEquals(responseContent.length(), Integer.parseInt(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH).getValue()));

        verify(postRequestedFor(urlEqualTo("/team/my_team")).withRequestBody(equalToJson(mockContent)));
    }

    @Test
    public void small_body_with_chunked_transfer_encoding() throws Exception {
        String mockContent = StringUtils.copy(getClass().getClassLoader().getResourceAsStream("case1/response_content.json"));

        stubFor(
            post(urlEqualTo("/team/my_team"))
                .willReturn(
                    ok().withBody(mockContent).withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                )
        );

        Request request = Request
            .Post("http://localhost:8082/test/my_team")
            .bodyStream(this.getClass().getClassLoader().getResourceAsStream("case1/request_content.json"), ContentType.APPLICATION_JSON);
        HttpResponse response = execute(request).returnResponse();

        String responseContent = StringUtils.copy(response.getEntity().getContent());

        assertEquals(mockContent, responseContent);
        assertNull(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH));
        assertNotNull(response.getFirstHeader(HttpHeaders.TRANSFER_ENCODING));
        assertEquals(HttpHeadersValues.TRANSFER_ENCODING_CHUNKED, response.getFirstHeader(HttpHeaders.TRANSFER_ENCODING).getValue());

        verify(postRequestedFor(urlEqualTo("/team/my_team")).withRequestBody(equalToJson(mockContent)));
    }

    @Test
    public void large_body_with_chunked_transfer_encoding() throws Exception {
        String mockContent = StringUtils.copy(getClass().getClassLoader().getResourceAsStream("case2/response_content.json"));

        stubFor(
            post(urlEqualTo("/team/my_team"))
                .willReturn(
                    ok().withBody(mockContent).withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                )
        );

        Request request = Request
            .Post("http://localhost:8082/test/my_team")
            .bodyStream(getClass().getClassLoader().getResourceAsStream("case2/response_content.json"), ContentType.APPLICATION_JSON);

        HttpResponse response = execute(request).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String content = StringUtils.copy(response.getEntity().getContent());
        assertEquals(652051, content.length());
        assertEquals(content, content);
        assertNull(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH));
        assertNotNull(response.getFirstHeader(HttpHeaders.TRANSFER_ENCODING));
        assertEquals(HttpHeadersValues.TRANSFER_ENCODING_CHUNKED, response.getFirstHeader(HttpHeaders.TRANSFER_ENCODING).getValue());

        System.out.println(wireMockRule.findAllUnmatchedRequests().size());
        verify(postRequestedFor(urlEqualTo("/team/my_team")).withRequestBody(equalToJson(mockContent)));
    }

    @Test
    public void large_body_with_content_length() throws Exception {
        String mockContent = StringUtils.copy(getClass().getClassLoader().getResourceAsStream("case2/response_content.json"));

        stubFor(
            post(urlEqualTo("/team/my_team"))
                .willReturn(
                    ok().withBody(mockContent).withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                )
        );

        Request request = Request.Post("http://localhost:8082/test/my_team").bodyString(mockContent, ContentType.APPLICATION_JSON);

        HttpResponse response = execute(request).returnResponse();

        System.out.println(wireMockRule.findAllUnmatchedRequests().size());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        String content = StringUtils.copy(response.getEntity().getContent());
        assertEquals(652051, content.length());
        assertEquals(content, content);
        assertNull(response.getFirstHeader(HttpHeaders.CONTENT_LENGTH));
        assertNotNull(response.getFirstHeader(HttpHeaders.TRANSFER_ENCODING));
        assertEquals(HttpHeadersValues.TRANSFER_ENCODING_CHUNKED, response.getFirstHeader(HttpHeaders.TRANSFER_ENCODING).getValue());

        verify(postRequestedFor(urlEqualTo("/team/my_team")).withRequestBody(equalTo(mockContent)));
    }

    @Test
    public void no_content_with_chunked_encoding_transfer() throws Exception {
        stubFor(post(urlEqualTo("/team/my_team")).willReturn(ok()));

        Request request = Request.Post("http://localhost:8082/test/my_team");

        HttpResponse response = execute(request).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Set chunk mode in request but returns raw because of the size of the content
        assertEquals(null, response.getFirstHeader("X-Forwarded-Transfer-Encoding"));

        String responseContent = StringUtils.copy(response.getEntity().getContent());
        assertEquals(0, responseContent.length());

        verify(postRequestedFor(urlEqualTo("/team/my_team")).withoutHeader(HttpHeaders.TRANSFER_ENCODING));
    }

    @Test
    public void no_content_without_chunked_encoding_transfer() throws Exception {
        stubFor(post(urlEqualTo("/team/my_team")).willReturn(ok()));

        Request request = Request
            .Post("http://localhost:8082/test/my_team")
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .removeHeaders(HttpHeaders.TRANSFER_ENCODING);

        Response response = execute(request);

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        // Set chunk mode in request but returns raw because of the size of the content
        assertEquals(null, returnResponse.getFirstHeader("X-Forwarded-Transfer-Encoding"));

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(0, responseContent.length());

        verify(
            postRequestedFor(urlEqualTo("/team/my_team"))
                .withoutHeader(HttpHeaders.TRANSFER_ENCODING)
                .withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, new EqualToPattern(MediaType.APPLICATION_JSON))
        );
    }

    @Test
    public void get_no_content_with_chunked_encoding_transfer() throws Exception {
        stubFor(get(urlEqualTo("/team/my_team")).willReturn(ok()));

        Request request = Request
            .Get("http://localhost:8082/test/my_team")
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .removeHeaders(HttpHeaders.TRANSFER_ENCODING);

        Response response = execute(request);

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        // Set chunk mode in request but returns raw because of the size of the content
        assertEquals(null, returnResponse.getFirstHeader("X-Forwarded-Transfer-Encoding"));

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(0, responseContent.length());

        verify(
            getRequestedFor(urlEqualTo("/team/my_team"))
                .withoutHeader(HttpHeaders.TRANSFER_ENCODING)
                .withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, new EqualToPattern(MediaType.APPLICATION_JSON))
        );
    }

    @Test
    public void get_no_content_with_chunked_encoding_transfer_and_content_type() throws Exception {
        stubFor(get(urlEqualTo("/team/my_team")).willReturn(ok()));

        Request request = Request.Get("http://localhost:8082/test/my_team").addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = execute(request);

        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        // Set chunk mode in request but returns raw because of the size of the content
        assertEquals(null, returnResponse.getFirstHeader("X-Forwarded-Transfer-Encoding"));

        String responseContent = StringUtils.copy(returnResponse.getEntity().getContent());
        assertEquals(0, responseContent.length());

        verify(
            getRequestedFor(urlEqualTo("/team/my_team"))
                .withHeader(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, new EqualToPattern(MediaType.APPLICATION_JSON))
        );
    }
}
