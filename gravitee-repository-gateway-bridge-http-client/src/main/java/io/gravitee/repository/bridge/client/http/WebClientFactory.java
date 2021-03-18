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
package io.gravitee.repository.bridge.client.http;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.Version;
import io.gravitee.repository.bridge.client.utils.BridgePath;
import io.gravitee.repository.bridge.client.utils.VertxCompletableFuture;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.*;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.HttpContext;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebClientFactory implements FactoryBean<WebClient> {

    private final Logger logger = LoggerFactory.getLogger(WebClientFactory.class);

    private static final String KEYSTORE_FORMAT_JKS = "JKS";
    private static final String KEYSTORE_FORMAT_PEM = "PEM";
    private static final String KEYSTORE_FORMAT_PKCS12 = "PKCS12";

    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    @Autowired
    private Environment environment;

    @Autowired
    private Vertx vertx;

    private final String propertyPrefix;

    private CircuitBreaker circuitBreaker;

    public WebClientFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".http.";
    }

    private static final long retryDuration = 5000L;

    @Override
    public WebClient getObject() throws Exception {
        WebClientOptions options = getWebClientOptions();

        WebClientInternal client = (WebClientInternal) WebClient.create(vertx, options);

        client.addInterceptor(new ReadTimeoutInterceptor(WebClientFactory.this.readPropertyValue("readTimeout", Long.class, 10000L)));

        String username = readPropertyValue(propertyPrefix + "authentication.basic.username", String.class);
        String password = readPropertyValue(propertyPrefix + "authentication.basic.password", String.class);

        if (username != null && password != null) {
            client.addInterceptor(new BasicAuthorizationInterceptor(username, password));
        }

        circuitBreaker = CircuitBreaker.create(
                "cb-repository-bridge-client",
                vertx,
                new CircuitBreakerOptions()
                        .setMaxRetries(Integer.MAX_VALUE)
                        .setTimeout(2000))
        .retryPolicy(retryCount -> retryDuration);

        VertxCompletableFuture<WebClientInternal> completableConnection = VertxCompletableFuture.from(vertx, validateConnection(client));
        if (completableConnection.isCompletedExceptionally()) {
            throw new IllegalStateException("Unable to connect to the bridge server.");
        }

        return completableConnection.get();
    }

    private Future<WebClientInternal> validateConnection(WebClientInternal client) {
        logger.info("Validate Bridge Server connection ...");
        return circuitBreaker.execute(
                future -> client.get(BridgePath.get(environment)).as(BodyCodec.string()).send(response -> {
                    if (response.succeeded()) {
                        HttpResponse<String> httpResponse = response.result();

                        if (httpResponse.statusCode() == HttpStatusCode.OK_200) {
                            JsonObject jsonObject = new JsonObject(httpResponse.body());
                            JsonObject version = jsonObject.getJsonObject("version");
                            if (version == null || !version.containsKey("MAJOR_VERSION")) {
                                String msg = "Invalid format response from Bridge Server. Retry.";
                                logger.error(msg);
                                future.fail(msg);
                            } else {
                                logger.info("Bridge connection successful.");
                                future.complete(client);
                            }
                        } else {
                            String msg = String.format("Invalid Bridge Server response. Retry in %s ms.", retryDuration);
                            logger.error(msg);
                            future.fail(msg);
                        }
                    } else {
                        String msg = String.format("Unable to connect to the Bridge Server. Retry in %s ms.", retryDuration);
                        logger.error(msg);
                        future.fail(msg);
                    }
                }));
    }

    private WebClientOptions getWebClientOptions() {
        WebClientOptions options = new WebClientOptions()
                .setUserAgent("gio-client-bridge/" + Version.RUNTIME_VERSION.MAJOR_VERSION);

        // Add support for proxy
        options.setProxyOptions(getProxyOptions());

        options
                .setKeepAlive(readPropertyValue(propertyPrefix + "keepAlive", Boolean.class, true))
                .setIdleTimeout(readPropertyValue(propertyPrefix + "idleTimeout", Integer.class, 30000))
                .setConnectTimeout(readPropertyValue(propertyPrefix + "connectTimeout", Integer.class, 10000));

        String url = readPropertyValue(propertyPrefix + "url");
        final URI uri = URI.create(url);

        options
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort() != -1 ? uri.getPort() :
                        (HTTPS_SCHEME.equals(uri.getScheme()) ? 443 : 80));

        if (HTTPS_SCHEME.equals(uri.getScheme())) {
            options.setSsl(true);
            options.setTrustAll(readPropertyValue(propertyPrefix + "ssl.trustall", Boolean.class, true));
            options.setVerifyHost(readPropertyValue(propertyPrefix + "ssl.verifyHostname", Boolean.class, true));

            String keyStoreType = readPropertyValue(propertyPrefix + "ssl.keystore.type");

            if (keyStoreType != null) {
                if (keyStoreType.equalsIgnoreCase(KEYSTORE_FORMAT_JKS)) {
                    options.setKeyStoreOptions(
                            new JksOptions()
                                    .setPath(readPropertyValue(propertyPrefix + "ssl.keystore.path"))
                                    .setPassword(readPropertyValue(propertyPrefix + "ssl.keystore.password"))
                    );
                } else if (keyStoreType.equalsIgnoreCase(KEYSTORE_FORMAT_PKCS12)) {
                    options.setPfxKeyCertOptions(
                            new PfxOptions()
                                    .setPath(readPropertyValue(propertyPrefix + "ssl.keystore.path"))
                                    .setPassword(readPropertyValue(propertyPrefix + "ssl.keystore.password"))
                    );
                } else if (keyStoreType.equalsIgnoreCase(KEYSTORE_FORMAT_PEM)) {
                    options.setPemKeyCertOptions(
                            new PemKeyCertOptions()
                                    .setCertPaths(getArrayValues(propertyPrefix + "ssl.keystore.certs"))
                                    .setKeyPaths(getArrayValues(propertyPrefix + "ssl.keystore.keys"))
                    );
                }
            }

            String trustStoreType = readPropertyValue(propertyPrefix + "ssl.truststore.type");

            if (trustStoreType != null) {
                if (trustStoreType.equalsIgnoreCase(KEYSTORE_FORMAT_JKS)) {
                    options.setTrustStoreOptions(
                            new JksOptions()
                                    .setPath(readPropertyValue(propertyPrefix + "ssl.truststore.path"))
                                    .setPassword(readPropertyValue(propertyPrefix + "ssl.truststore.password"))
                    );
                } else if (trustStoreType.equalsIgnoreCase(KEYSTORE_FORMAT_PKCS12)) {
                    options.setPfxTrustOptions(
                            new PfxOptions()
                                    .setPath(readPropertyValue(propertyPrefix + "ssl.truststore.path"))
                                    .setPassword(readPropertyValue(propertyPrefix + "ssl.truststore.password"))
                    );
                } else if (trustStoreType.equalsIgnoreCase(KEYSTORE_FORMAT_PEM)) {
                    options.setPemTrustOptions(
                            new PemTrustOptions()
                                    .addCertPath(readPropertyValue(propertyPrefix + "ssl.truststore.path"))
                    );
                }
            }
        }

        return options;
    }

    private ProxyOptions getProxyOptions() {
        if (environment.containsProperty(propertyPrefix + "proxy.host")) {
            ProxyOptions proxyOptions = new ProxyOptions();

            proxyOptions.setHost(readPropertyValue(propertyPrefix + "proxy.host"));
            proxyOptions.setPort(readPropertyValue(propertyPrefix + "proxy.port", Integer.class));
            proxyOptions.setType(ProxyType.valueOf(readPropertyValue(propertyPrefix + "proxy.type", String.class, ProxyType.HTTP.toString())));
            proxyOptions.setUsername(readPropertyValue(propertyPrefix + "proxy.username"));
            proxyOptions.setPassword(readPropertyValue(propertyPrefix + "proxy.password"));

            return proxyOptions;
        }

        return null;
    }

    private List<String> getArrayValues(String property) {
        String key = String.format(property, 0);
        List<String> values = new ArrayList<>();

        while (environment.containsProperty(key)) {
            values.add(environment.getProperty(key));
            key = String.format(property, values.size());
        }

        return values;
    }

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, String.class, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }

    @Override
    public Class<?> getObjectType() {
        return WebClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private static class ReadTimeoutInterceptor implements Handler<HttpContext<?>> {
        private final long timeout;

        ReadTimeoutInterceptor(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public void handle(HttpContext context) {
            context.request().timeout(timeout);
            context.next();
        }
    }

    private static class BasicAuthorizationInterceptor implements Handler<HttpContext<?>> {
        private final String authorizationSchemeValue;
        private final static String AUTHORIZATION_SCHEME = "Basic ";

        BasicAuthorizationInterceptor(String username, String password) {
            authorizationSchemeValue = AUTHORIZATION_SCHEME + Base64.getEncoder().encodeToString((username + ':' + password).getBytes());
        }

        @Override
        public void handle(HttpContext context) {
            context.request().putHeader(HttpHeaders.AUTHORIZATION, authorizationSchemeValue);
            context.next();
        }
    }
}
