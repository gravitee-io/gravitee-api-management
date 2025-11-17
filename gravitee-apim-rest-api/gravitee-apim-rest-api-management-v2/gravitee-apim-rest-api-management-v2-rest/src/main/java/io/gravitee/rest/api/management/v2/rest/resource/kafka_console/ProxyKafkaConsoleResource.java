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
package io.gravitee.rest.api.management.v2.rest.resource.kafka_console;

import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.use_case.SearchClusterUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ProxyKafkaConsoleResource extends AbstractResource {

    @Inject
    private SearchClusterUseCase searchClusterUseCase;

    @Inject
    private UserService userService;

    @Inject
    private Environment environment;

    @Inject
    private Vertx vertx;

    private final List<String> headerNamesToRemove = List.of("Authorization");

    private @NotNull MultivaluedMap<String, String> getFilteredHeaders(HttpServletRequest httpRequest) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        httpRequest
            .getHeaderNames()
            .asIterator()
            .forEachRemaining(headerName -> {
                if (!headerNamesToRemove.contains(headerName)) {
                    headers.putSingle(headerName, httpRequest.getHeader(headerName));
                }
            });
        return headers;
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public void proxyKafkaConsolePost(@Suspended AsyncResponse finalResponse, @Context HttpServletRequest httpRequest, Buffer body) {
        proxyKafkaConsole(finalResponse, httpRequest, HttpMethod.POST, body);
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public void proxyKafkaConsoleGet(@Suspended AsyncResponse finalResponse, @Context HttpServletRequest httpRequest) {
        proxyKafkaConsole(finalResponse, httpRequest, HttpMethod.GET, null);
    }

    @POST
    @Path("/{s:.*}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public void proxyKafkaConsolePostSubResource(
        @Suspended AsyncResponse finalResponse,
        @Context HttpServletRequest httpRequest,
        Buffer body
    ) {
        proxyKafkaConsole(finalResponse, httpRequest, HttpMethod.POST, body);
    }

    @GET
    @Path("/{s:.*}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public void proxyKafkaConsoleGetSubResource(@Suspended AsyncResponse finalResponse, @Context HttpServletRequest httpRequest) {
        proxyKafkaConsole(finalResponse, httpRequest, HttpMethod.GET, null);
    }

    private void proxyKafkaConsole(AsyncResponse finalResponse, HttpServletRequest httpRequest, HttpMethod httpMethod, Buffer body) {
        Boolean enabled = environment.getProperty("kafka.console.enabled", Boolean.class, Boolean.FALSE);
        if (!enabled) {
            log.warn("Kafka console server security secret is not defined. Please define it in your environment.");
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
            finalResponse.resume(responseBuilder.build());
            return;
        }
        String jwtSecret = environment.getProperty("kafka.console.server.security.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.warn("Kafka console server security secret is not defined. Please define it in your environment.");
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
            finalResponse.resume(responseBuilder.build());
            return;
        }

        String kafbatServerScheme = environment.getProperty("kafka.console.server.scheme", "http");
        String kafbatServerHost = environment.getProperty("kafka.console.server.host", "localhost");
        Integer kafbatServerPort = environment.getProperty("kafka.console.server.port", Integer.class, 8080);

        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(kafbatServerHost);
        options.setDefaultPort(kafbatServerPort);
        if (kafbatServerScheme.equalsIgnoreCase("https")) {
            options.setSsl(true);
            options.setTrustAll(true);
        }

        HttpClient httpClient = vertx.createHttpClient(options);

        String kafbatToken = computeKafbatToken(jwtSecret);

        String requestURI = httpRequest.getRequestURI();
        String baseURI = requestURI.substring(0, requestURI.indexOf("proxy-kafka-console") + "proxy-kafka-console".length());
        final String kafbatURI =
            requestURI.substring(baseURI.length()) + (httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString());

        Future<HttpClientRequest> requestFuture = httpClient.request(httpMethod, kafbatURI);
        requestFuture
            .onFailure(throwable -> {
                finalResponse.resume(throwable);

                // Close client
                httpClient.close();
            })
            .onSuccess(request -> {
                getFilteredHeaders(httpRequest).forEach(request::putHeader);
                request.putHeader("Authorization", kafbatToken);
                request
                    .response(asyncResponse -> {
                        if (asyncResponse.failed()) {
                            finalResponse.resume(asyncResponse.cause());

                            // Close client
                            httpClient.close();
                        } else {
                            HttpClientResponse response = asyncResponse.result();

                            response.bodyHandler(buffer -> {
                                Response.ResponseBuilder responseBuilder;
                                if (
                                    response.headers().get(HttpHeaderNames.CONTENT_TYPE) != null &&
                                    response.headers().get(HttpHeaderNames.CONTENT_TYPE).startsWith("image")
                                ) {
                                    responseBuilder = Response.ok(buffer.getBytes());
                                    response.headers().forEach(header -> responseBuilder.header(header.getKey(), header.getValue()));
                                } else {
                                    String payload;
                                    payload = buffer.toString();

                                    payload = payload.replace(
                                        """
                                        href="/""",
                                        """
                                        href="%s/""".formatted(baseURI)
                                    );

                                    payload = payload.replace(
                                        """
                                        src="/""",
                                        """
                                        src="%s/""".formatted(baseURI)
                                    );

                                    payload = payload.replace(
                                        """
                                        src: url('/""",
                                        """
                                        src: url('%s/""".formatted(baseURI)
                                    );

                                    payload = payload.replace(
                                        """
                                        window.basePath = '';""",
                                        """
                                        window.basePath = '%s';""".formatted(baseURI)
                                    );

                                    responseBuilder = Response.status(response.statusCode()).entity(payload);
                                    response
                                        .headers()
                                        .forEach(header -> {
                                            if (!header.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH.toString())) {
                                                responseBuilder.header(header.getKey(), header.getValue());
                                            }
                                        });
                                    responseBuilder.header("content-length", payload.getBytes(StandardCharsets.UTF_8).length);
                                }
                                finalResponse.resume(responseBuilder.build());

                                // Close client
                                httpClient.close();
                            });
                        }
                    })
                    .exceptionHandler(throwable -> {
                        finalResponse.resume(throwable);

                        // Close client
                        httpClient.close();
                    });
                if (body != null) {
                    request.setChunked(true);
                    request.write(body);
                }
                request.end();
            });
    }

    private String computeKafbatToken(String jwtSecret) {
        // Get available clusters for current user
        var executionContext = GraviteeContext.getExecutionContext();
        SearchClusterUseCase.Output result = searchClusterUseCase.execute(
            new SearchClusterUseCase.Input(
                executionContext.getEnvironmentId(),
                null,
                new PageableImpl(1, 1000),
                "name",
                isAdmin(),
                getAuthenticatedUser()
            )
        );
        List<String> listOfAvailableClusters = result.pageResult().getContent().stream().map(Cluster::getName).toList();

        //Get current user identity
        UserEntity currentUser = userService.findById(executionContext, getAuthenticatedUser());

        // generate a JWT to store user's information and for security purpose
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);

        Date issueAt = new Date();
        Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER));

        final String token = JWT.create()
            .withIssuer(DEFAULT_JWT_ISSUER)
            .withIssuedAt(issueAt)
            .withExpiresAt(Date.from(expireAt))
            .withSubject(currentUser.getDisplayName())
            .withClaim("clusters", listOfAvailableClusters)
            .sign(algorithm);

        return "Bearer " + token;
    }
}
