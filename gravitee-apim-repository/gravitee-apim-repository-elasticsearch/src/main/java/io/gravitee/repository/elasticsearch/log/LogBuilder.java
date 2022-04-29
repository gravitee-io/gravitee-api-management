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
package io.gravitee.repository.elasticsearch.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.log.model.Log;
import io.gravitee.repository.log.model.Request;
import io.gravitee.repository.log.model.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for log request.
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 */
final class LogBuilder {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(LogBuilder.class);

    /** Document simple date format **/
    private static SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final String FIELD_TRANSACTION_ID = "transaction";
    private static final String FIELD_TIMESTAMP = "@timestamp";
    private static final String FIELD_GATEWAY = "gateway";

    private static final String FIELD_METHOD = "method";
    private static final String FIELD_URI = "uri";
    private static final String FIELD_ENDPOINT = "endpoint";
    private static final String FIELD_REQUEST_CONTENT_LENGTH = "request-content-length";
    private static final String FIELD_RESPONSE_CONTENT_LENGTH = "response-content-length";
    private static final String FIELD_CLIENT_REQUEST = "client-request";
    private static final String FIELD_PROXY_REQUEST = "proxy-request";
    private static final String FIELD_CLIENT_RESPONSE = "client-response";
    private static final String FIELD_PROXY_RESPONSE = "proxy-response";
    private static final String FIELD_BODY = "body";
    private static final String FIELD_HEADERS = "headers";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESPONSE_TIME = "response-time";
    private static final String FIELD_API_RESPONSE_TIME = "api-response-time";

    private static final String FIELD_LOCAL_ADDRESS = "local-address";
    private static final String FIELD_REMOTE_ADDRESS = "remote-address";

    private static final String FIELD_TENANT = "tenant";
    private static final String FIELD_APPLICATION = "application";
    private static final String FIELD_API = "api";
    private static final String FIELD_PLAN = "plan";
    private static final String FIELD_HOST = "host";

    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_USER = "user";

    private static final String FIELD_SECURITY_TYPE = "security-type";
    private static final String FIELD_SECURITY_TOKEN = "security-token";

    private static final String FIELD_ERROR_KEY = "error-key";

    static Log createLog(final SearchHit hit) {
        return createLog(hit, new Log());
    }

    static ExtendedLog createExtendedLog(final SearchHit hit, final JsonNode log) {
        ExtendedLog extentedLog = createLog(hit, new ExtendedLog());

        // Add client and proxy requests / responses
        if (log != null) {
            extentedLog.setClientRequest(createRequest(log.get(FIELD_CLIENT_REQUEST)));
            extentedLog.setProxyRequest(createRequest(log.get(FIELD_PROXY_REQUEST)));
            extentedLog.setClientResponse(createResponse(log.get(FIELD_CLIENT_RESPONSE)));
            extentedLog.setProxyResponse(createResponse(log.get(FIELD_PROXY_RESPONSE)));
        }

        return extentedLog;
    }

    private static <T extends Log> T createLog(final SearchHit hit, final T log) {
        final JsonNode source = hit.getSource();
        log.setId(hit.getId());
        log.setTransactionId(source.get(FIELD_TRANSACTION_ID).asText());
        log.setGateway(source.get(FIELD_GATEWAY).asText());

        try {
            log.setTimestamp(dtf.parse((source.get(FIELD_TIMESTAMP).asText())).getTime());
        } catch (final ParseException e) {
            logger.error("Impossible to parse date", e);
            throw new IllegalArgumentException("Impossible to parse timestamp field", e);
        }

        log.setUri(source.get(FIELD_URI).asText());

        JsonNode methodNode = source.get(FIELD_METHOD);

        if (methodNode.canConvertToInt()) {
            log.setMethod(HttpMethod.get(methodNode.asInt()));
        } else {
            log.setMethod(HttpMethod.valueOf(methodNode.asText()));
        }

        log.setStatus(source.get(FIELD_STATUS).asInt());
        log.setResponseTime(source.get(FIELD_RESPONSE_TIME).asLong());
        log.setApiResponseTime(source.path(FIELD_API_RESPONSE_TIME).asLong());
        log.setRequestContentLength(source.path(FIELD_REQUEST_CONTENT_LENGTH).asLong());
        log.setResponseContentLength(source.path(FIELD_RESPONSE_CONTENT_LENGTH).asLong());
        log.setLocalAddress(source.get(FIELD_LOCAL_ADDRESS).asText());
        log.setRemoteAddress(source.get(FIELD_REMOTE_ADDRESS).asText());

        final JsonNode tenantNode = source.get(FIELD_TENANT);
        if (tenantNode != null && !tenantNode.isNull()) {
            log.setTenant(tenantNode.asText());
        }

        final JsonNode applicationNode = source.get(FIELD_APPLICATION);
        if (applicationNode != null && !applicationNode.isNull()) {
            log.setApplication(applicationNode.asText());
        }

        final JsonNode apiNode = source.get(FIELD_API);
        if (apiNode != null && !apiNode.isNull()) {
            log.setApi(apiNode.asText());
        }

        final JsonNode planNode = source.get(FIELD_PLAN);
        if (planNode != null && !planNode.isNull()) {
            log.setPlan(planNode.asText());
        }

        final JsonNode endpointNode = source.get(FIELD_ENDPOINT);
        if (endpointNode != null && !endpointNode.isNull()) {
            log.setEndpoint(endpointNode.asText());
        }

        final JsonNode messageNode = source.get(FIELD_MESSAGE);
        if (messageNode != null && !messageNode.isNull()) {
            log.setMessage(messageNode.asText());
        }

        final JsonNode hostNode = source.get(FIELD_HOST);
        if (hostNode != null && !hostNode.isNull()) {
            log.setHost(hostNode.asText());
        }

        final JsonNode userNode = source.get(FIELD_USER);
        if (userNode != null && !userNode.isNull()) {
            log.setUser(userNode.asText());
        }

        final JsonNode secTypeNode = source.get(FIELD_SECURITY_TYPE);
        if (secTypeNode != null && !secTypeNode.isNull()) {
            log.setSecurityType(secTypeNode.asText());
        }

        final JsonNode secTokenNode = source.get(FIELD_SECURITY_TOKEN);
        if (secTokenNode != null && !secTokenNode.isNull()) {
            log.setSecurityToken(secTokenNode.asText());
        }

        final JsonNode errorKeyNode = source.get(FIELD_ERROR_KEY);
        if (errorKeyNode != null && !errorKeyNode.isNull()) {
            log.setErrorKey(errorKeyNode.asText());
        }

        return log;
    }

    private static Request createRequest(final JsonNode node) {
        if (node == null) {
            return null;
        }

        Request request = new Request();
        request.setUri(node.path(FIELD_URI).asText());

        if (node.get(FIELD_METHOD) != null) {
            request.setMethod(HttpMethod.valueOf(node.get(FIELD_METHOD).asText()));
        }

        if (node.get(FIELD_BODY) != null) {
            request.setBody(node.get(FIELD_BODY).asText());
        }

        request.setHeaders(createHttpHeaders(node.get(FIELD_HEADERS)));
        return request;
    }

    private static Response createResponse(final JsonNode node) {
        if (node == null) {
            return null;
        }

        Response response = new Response();
        response.setStatus(node.path(FIELD_STATUS).asInt());
        if (node.get(FIELD_BODY) != null) {
            response.setBody(node.get(FIELD_BODY).asText());
        }
        response.setHeaders(createHttpHeaders(node.get(FIELD_HEADERS)));
        return response;
    }

    private static HttpHeaders createHttpHeaders(final JsonNode node) {
        if (node == null) {
            return null;
        }

        HttpHeaders httpHeaders = new HttpHeaders();

        final Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            final String name = iterator.next();
            final ArrayNode values = (ArrayNode) node.get(name);
            httpHeaders.put(name, convertToList(values));
        }

        return httpHeaders;
    }

    private static List<String> convertToList(ArrayNode values) {
        final List<String> result = new ArrayList<>(values.size());
        values.forEach(jsonNode -> result.add(jsonNode.asText()));
        return result;
    }
}
