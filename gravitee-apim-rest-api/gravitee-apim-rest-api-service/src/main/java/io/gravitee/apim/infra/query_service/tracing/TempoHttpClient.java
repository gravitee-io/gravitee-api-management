/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.infra.query_service.tracing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP client for Grafana Tempo. POC scope: single Tempo backend. Base URL is read from the {@code tracing.tempo.url} Spring property
 * (default {@code http://localhost:3200}) — see {@link TempoTracingQueryService}.
 */
@Slf4j
class TempoHttpClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String tempoBaseUrl;

    TempoHttpClient(String tempoBaseUrl) {
        this.tempoBaseUrl = tempoBaseUrl;
        log.info("Tempo base URL: {}", tempoBaseUrl);
    }

    TempoTraceResponse getTrace(String traceId) {
        return executeGet(tempoBaseUrl + "/api/traces/" + traceId, TempoTraceResponse.class);
    }

    TempoSearchResponse searchTracesTraceQL(String traceQL, Integer limit, Long start, Long end) {
        StringBuilder url = new StringBuilder(tempoBaseUrl).append("/api/search");
        String separator = "?";

        if (traceQL != null) {
            url.append(separator).append("q=").append(URLEncoder.encode(traceQL, StandardCharsets.UTF_8));
            separator = "&";
        }
        if (limit != null) {
            url.append(separator).append("limit=").append(limit);
            separator = "&";
        }
        if (start != null) {
            url.append(separator).append("start=").append(start);
            separator = "&";
        }
        if (end != null) {
            url.append(separator).append("end=").append(end);
        }

        return executeGet(url.toString(), TempoSearchResponse.class);
    }

    private <T> T executeGet(String url, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Accept", "application/json").GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), responseType);
            }
            log.warn("Tempo API returned HTTP {} for {}", response.statusCode(), url);
            throw new TempoClientException("Tempo API returned HTTP " + response.statusCode());
        } catch (IOException e) {
            throw new TempoClientException("Error calling Tempo API: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TempoClientException("Interrupted calling Tempo API: " + url, e);
        }
    }

    static class TempoClientException extends RuntimeException {

        TempoClientException(String message) {
            super(message);
        }

        TempoClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
