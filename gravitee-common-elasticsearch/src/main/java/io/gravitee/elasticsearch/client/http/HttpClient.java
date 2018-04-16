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
package io.gravitee.elasticsearch.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.config.Endpoint;
import io.gravitee.elasticsearch.exception.ElasticsearchException;
import io.gravitee.elasticsearch.model.Health;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.bulk.BulkResponse;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.HttpContext;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClient implements Client {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private static final String HTTPS_SCHEME = "https";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON + ";charset=UTF-8";

    private static final String URL_ROOT = "/";
    private static final String URL_STATE_CLUSTER = "/_cluster/health";
    private static final String URL_BULK = "/_bulk";
    private static final String URL_TEMPLATE = "/_template";
    private static final String URL_INGEST = "/_ingest/pipeline";
    private static final String URL_SEARCH = "/_search?ignore_unavailable=true";

    @Autowired
    private Vertx vertx;

    /**
     * Configuration of Elasticsearch (cluster name, addresses, ...)
     */
    private HttpClientConfiguration configuration;

    /**
     * HTTP client.
     */
    private WebClient httpClient;

    /**
     * Authorization header if Elasticsearch is protected.
     */
    private String authorizationHeader;

    private final ObjectMapper mapper = new ObjectMapper();

    public HttpClient() {
        this(new HttpClientConfiguration());
    }

    public HttpClient(final HttpClientConfiguration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    public void initialize() {
        if (! configuration.getEndpoints().isEmpty()) {
            final Endpoint endpoint = configuration.getEndpoints().get(0);
            final URI elasticEdpt = URI.create(endpoint.getUrl());

            WebClientOptions options = new WebClientOptions()
                    .setDefaultHost(elasticEdpt.getHost())
                    .setDefaultPort(elasticEdpt.getPort() != -1 ? elasticEdpt.getPort() :
                            (HTTPS_SCHEME.equals(elasticEdpt.getScheme()) ? 443 : 80));

            if (HTTPS_SCHEME.equals(elasticEdpt.getScheme())) {
                options
                        .setSsl(true)
                        .setTrustAll(true);
            }

            this.httpClient = WebClient.create(vertx, options);

            // Read configuration to authenticate calls to Elasticsearch (basic authentication only)
            if (this.configuration.getUsername() != null) {
                this.authorizationHeader = this.initEncodedAuthorization(this.configuration.getUsername(),
                        this.configuration.getPassword());
            }

            ((WebClientInternal) this.httpClient.getDelegate()).addInterceptor(new Handler<HttpContext>() {
                @Override
                public void handle(HttpContext context) {
                    context.request()
                            .putHeader(HttpHeaders.ACCEPT, CONTENT_TYPE)
                            .putHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());

                    // Basic authentication
                    if (authorizationHeader != null) {
                        context.request().putHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
                    }

                    context.next();
                }
            });
        }
    }

    @Override
    public Single<Integer> getVersion() throws ElasticsearchException {
        return httpClient
                .get(URL_ROOT)
                .rxSend()
                .doOnError(throwable -> logger.error("Unable to get a connection to Elasticsearch", throwable))
                .map(response -> mapper.readTree(response.bodyAsString()).path("version").path("number").asText())
                .map(sVersion -> {
                    float result = Float.valueOf(sVersion.substring(0, 3));
                    int version = Integer.valueOf(sVersion.substring(0, 1));
                    if (result < 2) {
                        logger.warn("Please upgrade to Elasticsearch 2 or later. version={}", version);
                    }
                    return version;
                });
    }

    /**
     * Get the cluster health
     *
     * @return the cluster health
     * @throws ElasticsearchException error occurs during ES call
     */
    @Override
    public Single<Health> getClusterHealth() {
        return httpClient
                .get(URL_STATE_CLUSTER)
                .rxSend()
                .map(response -> mapper.readValue(response.bodyAsString(), Health.class));
    }

    @Override
    public Single<BulkResponse> bulk(final List<String> data) {
        if (data != null && !data.isEmpty()) {
            String content = data.stream().collect(Collectors.joining());

            return httpClient
                    .post(URL_BULK)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/x-ndjson")
                    .rxSendBuffer(Buffer.buffer(content))
                    .map(response -> {
                        if (response.statusCode() != HttpStatusCode.OK_200) {
                            logger.error("Unable to bulk index data: status[{}] data[{}] response[{}]",
                                    response.statusCode(), content, response.body());
                            throw new ElasticsearchException("Unable to bulk index data");
                        }

                        return mapper.readValue(response.bodyAsString(), BulkResponse.class);
                    });
        }

        return Single.never();
    }

    @Override
    public Completable putTemplate(String templateName, String template) {
        return httpClient
                .put(URL_TEMPLATE + '/' + templateName)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .rxSendBuffer(Buffer.buffer(template))
                .flatMapCompletable(response -> {
                    if (response.statusCode() != HttpStatusCode.OK_200) {
                        logger.error("Unable to put template mapping: status[{}] template[{}] response[{}]",
                                response.statusCode(), template, response.body());
                        return Completable.error(new ElasticsearchException("Unable to put template mapping"));
                    }

                    return Completable.complete();
                });
    }

    /**
     * Perform an HTTP search query
     * @param indexes indexes names. If null search on all indexes
     * @param type document type separated by comma. If null search on all types
     * @param query json body query
     * @return elasticsearch response
     */
    public Single<SearchResponse> search(final String indexes, final String type, final String query) {
        // index can be null _search on all index
        final StringBuilder url = new StringBuilder()
                .append('/')
                .append(indexes);

        if (type != null) {
            url.append('/').append(type);
        }

        url.append(URL_SEARCH);

        return httpClient
                .post(url.toString())
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .rxSendBuffer(Buffer.buffer(query))
                .map(response -> {
                    if (response.statusCode() != HttpStatusCode.OK_200) {
                        logger.error("Unable to search: status[{}] query[{}] response[{}]",
                                response.statusCode(), query, response.body());
                        throw new ElasticsearchException("Unable to search");
                    }

                    return mapper.readValue(response.bodyAsString(), SearchResponse.class);
                });
    }

    @Override
    public Completable putPipeline(String pipelineName, String pipeline) {
        return httpClient
                .put(URL_INGEST + '/' + pipelineName)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .rxSendBuffer(Buffer.buffer(pipeline))
                .flatMapCompletable(response -> {
                    switch (response.statusCode()) {
                        case HttpStatusCode.OK_200:
                            return Completable.complete();
                        case HttpStatusCode.BAD_REQUEST_400:
                            logger.warn("Unable to create ES pipeline: {}", pipelineName);
                            break;
                        default:
                            logger.error("Unable to put pipeline: status[{}] pipeline[{}] response[{}]",
                                    response.statusCode(), pipeline, response.body());
                            break;
                    }

                    return Completable.error(new ElasticsearchException("Unable to create ES pipeline: " + pipelineName));
                });
    }

    /**
     * Create the Basic HTTP auth
     *
     * @param username
     *            username
     * @param password
     *            password
     * @return Basic auth string
     */
    private String initEncodedAuthorization(final String username, final String password) {
        final String auth = username + ":" + password;
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }

    public void setConfiguration(HttpClientConfiguration configuration) {
        this.configuration = configuration;
    }
}
