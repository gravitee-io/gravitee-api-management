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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.*;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.log.extended.Request;
import io.gravitee.rest.api.model.log.extended.Response;
import io.gravitee.rest.api.portal.rest.model.Log;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogMapperTest {

    private static final String LOG_API = "my-log-api";
    private static final String LOG_APPLICATION = "my-log-application";
    private static final String LOG_HOST = "my-log-host";
    private static final String LOG_PATH = "my-log-path";
    private static final String LOG_TRANSACTION = "my-log-transaction";
    private static final String LOG_USER = "my-log-user";
    private static final String LOG_ID = "my-log-id";
    private static final String LOG_PLAN = "my-log-plan";
    private static final String LOG_REQUEST_BODY = "my-log-request-body";
    private static final String LOG_REQUEST_URI = "my-log-request-uri";
    private static final String LOG_RESPONSE_BODY = "my-log-response-body";
    private static final String LOG_SECURITY_TOKEN = "my-log-security-token";
    private static final String LOG_SECURITY_TYPE = "my-log-security-type";

    private LogMapper logMapper = new LogMapper();

    @Test
    public void testConvertListItem() {
        //init
        ApplicationRequestItem applicationRequestItem = new ApplicationRequestItem();
        applicationRequestItem.setApi(LOG_API);
        applicationRequestItem.setId(LOG_ID);
        applicationRequestItem.setMethod(HttpMethod.CONNECT);
        applicationRequestItem.setPath(LOG_PATH);
        applicationRequestItem.setPlan(LOG_PLAN);
        applicationRequestItem.setResponseTime(1);
        applicationRequestItem.setStatus(1);
        applicationRequestItem.setTimestamp(1);
        applicationRequestItem.setTransactionId(LOG_TRANSACTION);
        applicationRequestItem.setUser(LOG_USER);

        //Test
        Log log = logMapper.convert(applicationRequestItem);
        assertNotNull(log);

        assertEquals(LOG_API, log.getApi());
        assertNull(log.getHost());
        assertEquals(LOG_ID, log.getId());
        assertNull(log.getMetadata());
        assertEquals(io.gravitee.rest.api.portal.rest.model.HttpMethod.CONNECT, log.getMethod());
        assertEquals(LOG_PATH, log.getPath());
        assertEquals(LOG_PLAN, log.getPlan());
        assertNull(log.getRequest());
        assertNull(log.getRequestContentLength());
        assertNull(log.getResponse());
        assertNull(log.getResponseContentLength());
        assertEquals(1L, log.getResponseTime().longValue());
        assertNull(log.getSecurityToken());
        assertNull(log.getSecurityType());
        assertEquals(1, log.getStatus().intValue());
        assertEquals(1L, log.getTimestamp().longValue());
        assertEquals(LOG_TRANSACTION, log.getTransactionId());
        assertEquals(LOG_USER, log.getUser());
    }

    @Test
    public void testConvertFull() {
        //init
        ApplicationRequest applicationRequest = new ApplicationRequest();
        applicationRequest.setApi(LOG_API);
        applicationRequest.setHost(LOG_HOST);
        applicationRequest.setId(LOG_APPLICATION);

        Map<String, Map<String, String>> metadata = new HashMap<String, Map<String, String>>();
        HashMap<String, String> appMetadata = new HashMap<String, String>();
        appMetadata.put(LOG_API, LOG_API);
        metadata.put(LOG_APPLICATION, appMetadata);
        applicationRequest.setMetadata(metadata);

        applicationRequest.setMethod(HttpMethod.CONNECT);
        applicationRequest.setPath(LOG_PATH);
        applicationRequest.setPlan(LOG_PLAN);

        Request request = new Request();
        request.setBody(LOG_REQUEST_BODY);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, LOG_ID);
        request.setHeaders(requestHeaders);
        request.setMethod(HttpMethod.CONNECT);
        request.setUri(LOG_REQUEST_URI);
        applicationRequest.setRequest(request);

        applicationRequest.setRequestContentLength(1L);

        Response response = new Response();
        response.setBody(LOG_RESPONSE_BODY);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.ACCEPT_CHARSET, LOG_ID);
        response.setHeaders(responseHeaders);
        response.setStatus(1);
        applicationRequest.setResponse(response);
        applicationRequest.setResponseContentLength(1L);

        applicationRequest.setResponseTime(1);

        applicationRequest.setSecurityToken(LOG_SECURITY_TOKEN);
        applicationRequest.setSecurityType(LOG_SECURITY_TYPE);
        applicationRequest.setStatus(1);
        applicationRequest.setTimestamp(1);
        applicationRequest.setTransactionId(LOG_TRANSACTION);
        applicationRequest.setUser(LOG_USER);

        //Test
        Log log = logMapper.convert(applicationRequest);
        assertNotNull(log);

        assertEquals(LOG_API, log.getApi());
        assertEquals(LOG_HOST, log.getHost());
        assertEquals(LOG_APPLICATION, log.getId());

        assertEquals(metadata, log.getMetadata());

        assertEquals(io.gravitee.rest.api.portal.rest.model.HttpMethod.CONNECT, log.getMethod());
        assertEquals(LOG_PATH, log.getPath());
        assertEquals(LOG_PLAN, log.getPlan());

        final io.gravitee.rest.api.portal.rest.model.Request logRequest = log.getRequest();
        assertNotNull(logRequest);
        assertEquals(LOG_REQUEST_BODY, logRequest.getBody());
        assertEquals(requestHeaders, logRequest.getHeaders());
        assertEquals(io.gravitee.rest.api.portal.rest.model.HttpMethod.CONNECT, logRequest.getMethod());
        assertEquals(LOG_REQUEST_URI, logRequest.getUri());

        assertEquals(1L, log.getRequestContentLength().longValue());

        final io.gravitee.rest.api.portal.rest.model.Response logResponse = log.getResponse();
        assertNotNull(logResponse);
        assertEquals(LOG_RESPONSE_BODY, logResponse.getBody());
        assertEquals(responseHeaders, logResponse.getHeaders());
        assertEquals(1, logResponse.getStatus().intValue());

        assertEquals(1L, log.getResponseContentLength().longValue());
        assertEquals(1L, log.getResponseTime().longValue());
        assertEquals(LOG_SECURITY_TOKEN, log.getSecurityToken());
        assertEquals(LOG_SECURITY_TYPE, log.getSecurityType());
        assertEquals(1, log.getStatus().intValue());
        assertEquals(1L, log.getTimestamp().longValue());
        assertEquals(LOG_TRANSACTION, log.getTransactionId());
        assertEquals(LOG_USER, log.getUser());
    }
}
