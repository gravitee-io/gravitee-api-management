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
import io.gravitee.common.util.Version;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.impl.HttpContext;
import io.vertx.ext.web.client.impl.WebClientInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.util.Base64;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebClientFactory implements FactoryBean<WebClient> {

    private final Logger logger = LoggerFactory.getLogger(WebClientFactory.class);

    private static final String HTTPS_SCHEME = "https";

    @Autowired
    private Environment environment;

    @Autowired
    private Vertx vertx;

    private final String propertyPrefix;

    public WebClientFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".http.";
    }

    @Override
    public WebClient getObject() throws Exception {
        WebClientOptions options = getWebClientOptions();

        WebClientInternal client = (WebClientInternal) WebClient.create(vertx, options);

        client.addInterceptor(new ReadTimeoutInterceptor());

        String username = readPropertyValue(propertyPrefix + "authentication.basic.username", String.class);
        String password = readPropertyValue(propertyPrefix + "authentication.basic.password", String.class);

        if (username != null && password != null) {
            client.addInterceptor(new BasicAuthorizationInterceptor(username, password));
        }

        return client;
    }

    private WebClientOptions getWebClientOptions() {
        WebClientOptions options = new WebClientOptions()
                .setUserAgent("gio-gw/" + Version.RUNTIME_VERSION.MAJOR_VERSION);

        options
                .setKeepAlive(readPropertyValue(propertyPrefix + "keepAlive", Boolean.class, true))
                .setIdleTimeout(readPropertyValue(propertyPrefix + "idleTimeout", Integer.class, 30000))
                .setConnectTimeout(readPropertyValue(propertyPrefix + "connectTimeout", Integer.class, 10000));

        String url = readPropertyValue(propertyPrefix + "url");
        final URI uri = URI.create(url);

        options.setDefaultHost(uri.getHost());
        options.setDefaultPort(uri.getPort() != -1 ? uri.getPort() :
                (HTTPS_SCHEME.equals(uri.getScheme()) ? 443 : 80));

        return options;
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

    private class ReadTimeoutInterceptor implements Handler<HttpContext<?>> {
        private final long timeout;

        ReadTimeoutInterceptor() {
            timeout = WebClientFactory.this.readPropertyValue("readTimeout", Long.class, 10000L);
        }

        @Override
        public void handle(HttpContext context) {
            context.request().timeout(timeout);
            context.next();
        }
    }

    private class BasicAuthorizationInterceptor implements Handler<HttpContext<?>> {
        private final String credentials;

        BasicAuthorizationInterceptor(String username, String password) {
            credentials = Base64.getEncoder().encodeToString((username + ':' + password).getBytes());
        }

        @Override
        public void handle(HttpContext context) {
            context.request().putHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
            context.next();
        }
    }
}
