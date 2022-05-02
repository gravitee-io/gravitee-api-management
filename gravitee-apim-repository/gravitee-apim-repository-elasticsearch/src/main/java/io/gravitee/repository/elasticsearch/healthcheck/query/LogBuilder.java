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
package io.gravitee.repository.elasticsearch.healthcheck.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.repository.healthcheck.query.log.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class LogBuilder {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(LogBuilder.class);

    /** Document simple date format **/
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final String FIELD_TIMESTAMP = "@timestamp";

    private static final String FIELD_GATEWAY = "gateway";
    private static final String FIELD_ENDPOINT = "endpoint";
    private static final String FIELD_RESPONSE_TIME = "response-time";
    private static final String FIELD_AVAILABLE = "available";
    private static final String FIELD_SUCCESS = "success";
    private static final String FIELD_STATE = "state";

    private static final String FIELD_STEPS = "steps";
    private static final String FIELD_METHOD = "method";
    private static final String FIELD_URI = "uri";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_MESSAGE = "message";

    private static final String FIELD_BODY = "body";
    private static final String FIELD_HEADERS = "headers";
    private static final String FIELD_RESPONSE = "response";
    private static final String FIELD_REQUEST = "request";

    static Log createLog(final SearchHit hit) {
        final JsonNode node = hit.getSource();
        final Log log = new Log();

        log.setId(hit.getId());
        log.setGateway(node.get(FIELD_GATEWAY).asText());

        try {
            log.setTimestamp(
                LocalDateTime.parse(node.get(FIELD_TIMESTAMP).asText(), dtf).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
        } catch (final DateTimeParseException e) {
            logger.error("Impossible to parse date", e);
            throw new IllegalArgumentException("Impossible to parse timestamp field", e);
        }

        log.setEndpoint(node.get(FIELD_ENDPOINT).asText());
        log.setResponseTime(node.get(FIELD_RESPONSE_TIME).asInt());
        log.setAvailable(node.get(FIELD_AVAILABLE).asBoolean());
        log.setState(node.get(FIELD_STATE).asInt());
        log.setSuccess(node.get(FIELD_SUCCESS).asBoolean());

        JsonNode steps = node.get(FIELD_STEPS);
        if (steps != null && steps.isArray() && steps.size() != 0) {
            JsonNode step = steps.get(0);
            JsonNode request = step.get(FIELD_REQUEST);
            JsonNode response = step.get(FIELD_RESPONSE);

            if (request != null) {
                log.setUri(request.get(FIELD_URI).asText());
                log.setMethod(HttpMethod.valueOf(request.get(FIELD_METHOD).asText()));
                log.setStatus(response.get(FIELD_STATUS).asInt());
            } else {
                // Ensure backward compatibility
                log.setUri(step.get(FIELD_URI).asText());
                log.setMethod(HttpMethod.valueOf(step.get(FIELD_METHOD).asText().toUpperCase()));
                log.setStatus(step.get(FIELD_STATUS).asInt());
            }
        }

        return log;
    }

    public static ExtendedLog createExtendedLog(final SearchHit hit) {
        final ExtendedLog log = new ExtendedLog();
        final JsonNode node = hit.getSource();

        log.setId(hit.getId());
        log.setGateway(node.get(FIELD_GATEWAY).asText());

        try {
            log.setTimestamp(
                LocalDateTime.parse(node.get(FIELD_TIMESTAMP).asText(), dtf).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
        } catch (final DateTimeParseException e) {
            logger.error("Impossible to parse date", e);
            throw new IllegalArgumentException("Impossible to parse timestamp field", e);
        }

        log.setEndpoint(node.get(FIELD_ENDPOINT).asText());
        log.setResponseTime(node.get(FIELD_RESPONSE_TIME).asInt());
        log.setAvailable(node.get(FIELD_AVAILABLE).asBoolean());
        log.setState(node.get(FIELD_STATE).asInt());
        log.setSuccess(node.get(FIELD_SUCCESS).asBoolean());

        JsonNode stepsNode = node.get(FIELD_STEPS);

        if (stepsNode != null && stepsNode.isArray()) {
            List<Step> steps = new ArrayList<>(stepsNode.size());
            for (JsonNode stepNode : stepsNode) {
                Step step = new Step();
                step.setSuccess(stepNode.get(FIELD_SUCCESS).asBoolean());
                step.setMessage(stepNode.get(FIELD_MESSAGE).asText());

                JsonNode requestNode = stepNode.get(FIELD_REQUEST);
                JsonNode responseNode = stepNode.get(FIELD_RESPONSE);

                if (requestNode != null) {
                    step.setRequest(createRequest(requestNode));
                    step.setResponse(createResponse(responseNode));
                } else {
                    // Ensure backward compatibility
                    Request request = new Request();
                    request.setUri(stepNode.get(FIELD_URI).asText());
                    request.setMethod(HttpMethod.valueOf(stepNode.get(FIELD_METHOD).asText().toUpperCase()));
                    step.setRequest(request);

                    Response response = new Response();
                    response.setStatus(stepNode.get(FIELD_STATUS).asInt());
                    step.setResponse(response);
                }
                steps.add(step);
            }
            log.setSteps(steps);
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
