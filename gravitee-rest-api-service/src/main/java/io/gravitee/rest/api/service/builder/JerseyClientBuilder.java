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
package io.gravitee.rest.api.service.builder;

import io.gravitee.common.util.EnvironmentUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import javax.ws.rs.client.ClientBuilder;
import java.net.*;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.gravitee.common.http.HttpHeaders.PROXY_AUTHORIZATION;

/**
 * This is an helper class which can be used to get a pre-configured jaxrs {@link ClientBuilder}.
 * The produced builder will be pre-configured with the following:
 * <ul>
 *     <li>Proxy settings based on gravitee <code>httpClient.proxy</code> configuration</li>.
 *     <li>Appropriate connection and read timeouts</li>
 * </ul>
 *
 * Notes:
 * This builder is only here to avoid duplicating code and can be viewed as a first step before we can provide an homogeneous way
 * to instantiate web clients for all Gravitee's components.
 * Note also, the JerseyClient will most probably be removed in favor of Vertx WebClient.
 * See https://github.com/gravitee-io/issues/issues/4728 for more details.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JerseyClientBuilder {

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final Pattern WILCARD_PATTERN = Pattern.compile("\\*\\.");

    /**
     * Prepare a {@link ClientBuilder} with proxy and timeout defined according to {@link Environment} configuration.
     *
     * @param environment the environment containing all the configuration.
     * @return a pre-configured {@link ClientBuilder} that can still be customized if needed.
     */
    public static ClientBuilder newBuilder(Environment environment) {
        final ClientBuilder builder = ClientBuilder.newBuilder();
        initHttpProxy(builder, environment);

        final int timeout = environment.getProperty("httpClient.timeout", Integer.class, 10000);
        builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(timeout, TimeUnit.MILLISECONDS);

        return builder;
    }

    private static void initHttpProxy(ClientBuilder builder, Environment environment) {

        final Proxy httpProxy = buildProxy(environment, HTTP_SCHEME);
        final String httpProxyAuth = buildProxyAuth(environment, HTTP_SCHEME);
        final Proxy httpsProxy = buildProxy(environment, HTTPS_SCHEME);
        final String httpsProxyAuth = buildProxyAuth(environment, HTTPS_SCHEME);

        final List<String> proxyExcludeHosts = EnvironmentUtils.getPropertiesStartingWith((ConfigurableEnvironment) environment, "httpClient.proxy.exclude-hosts").values().stream().map(String::valueOf).collect(Collectors.toList());


        final ClientConfig clientConfig = new ClientConfig()
                .connectorProvider(new HttpUrlConnectorProvider()
                        .connectionFactory(url -> {

                            final String host = url.getHost();
                            final boolean excluded = proxyExcludeHosts.stream().anyMatch(excludedHost -> {
                                if (excludedHost.startsWith("*.")) {
                                    return host.endsWith(WILCARD_PATTERN.matcher(excludedHost).replaceFirst(""));
                                } else {
                                    return host.equals(excludedHost);
                                }
                            });

                            if (excluded || (httpProxy == null && httpsProxy == null)) {
                                return (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                            }

                            final HttpURLConnection uc;

                            if (url.getProtocol().equals(HTTPS_SCHEME) && httpsProxy != null) {
                                uc = (HttpURLConnection) url.openConnection(httpsProxy);
                                if (useSockProxy(environment, HTTPS_SCHEME)) {
                                    buildSockProxyAuth(environment, HTTPS_SCHEME);
                                } else if (httpsProxyAuth != null) {
                                    uc.setRequestProperty(PROXY_AUTHORIZATION, "Basic " + httpsProxyAuth);
                                }
                            } else if (url.getProtocol().equals(HTTP_SCHEME) && httpProxy != null) {
                                uc = (HttpURLConnection) url.openConnection(httpProxy);
                                if (useSockProxy(environment, HTTP_SCHEME)) {
                                    buildSockProxyAuth(environment, HTTP_SCHEME);
                                } else if (httpProxyAuth != null) {
                                    uc.setRequestProperty(PROXY_AUTHORIZATION, "Basic " + httpProxyAuth);
                                }
                            } else {
                                uc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                            }

                            return uc;
                        }));

        builder.withConfig(clientConfig);
    }

    private static boolean useSockProxy(Environment environment, String type) {
        return environment.getProperty("httpClient.proxy.type", String.class, "HTTP").startsWith("SOCK");
    }

    private static Proxy buildProxy(Environment environment, String type) {

        final String proxyHost = environment.getProperty("httpClient.proxy." + type + ".host", String.class, environment.getProperty("http.proxyHost"));
        final Integer proxyPort = environment.getProperty("httpClient.proxy." + type + ".port", Integer.class, environment.getProperty("http.proxyPort", Integer.class, 3124));

        Proxy.Type pType = Proxy.Type.HTTP;

        if (useSockProxy(environment, type)) {
            pType = Proxy.Type.SOCKS;
        }

        if (proxyHost != null) {
            return new Proxy(pType, new InetSocketAddress(proxyHost, proxyPort));
        }

        return null;
    }

    private static String buildProxyAuth(Environment environment, String type) {

        final String proxyHttpUsername = environment.getProperty("httpClient.proxy." + type + ".username", String.class);
        final String proxyHttpPassword = environment.getProperty("httpClient.proxy." + type + ".password", String.class);

        return proxyHttpUsername != null ? new String(Base64.getEncoder().encode((proxyHttpUsername + ':' + proxyHttpPassword).getBytes())) : null;
    }

    private static void buildSockProxyAuth(Environment environment, String type) {

        final String proxyHttpUsername = environment.getProperty("httpClient.proxy." + type + ".username", String.class);
        final String proxyHttpPassword = environment.getProperty("httpClient.proxy." + type + ".password", String.class);

        if (proxyHttpUsername != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyHttpUsername, proxyHttpPassword != null ? proxyHttpPassword.toCharArray() : new char[0]);
                }
            });
        }
    }
}
