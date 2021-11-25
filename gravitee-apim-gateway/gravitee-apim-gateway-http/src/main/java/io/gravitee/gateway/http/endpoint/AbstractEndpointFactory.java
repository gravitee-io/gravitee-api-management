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
package io.gravitee.gateway.http.endpoint;

import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.core.endpoint.ManagedEndpoint;
import io.gravitee.gateway.core.endpoint.factory.template.TemplateAwareEndpointFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractEndpointFactory extends TemplateAwareEndpointFactory<io.gravitee.definition.model.Endpoint, ManagedEndpoint>
        implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(HttpEndpointFactory.class);

    protected ApplicationContext applicationContext;

    @Override
    protected io.gravitee.definition.model.endpoint.HttpEndpoint resolve(io.gravitee.definition.model.Endpoint endpoint) {
        HttpEndpoint httpEndpoint = (HttpEndpoint) endpoint;

        // HTTP endpoint configuration
        httpEndpoint.setTarget(convert(httpEndpoint.getTarget()));

        // HTTP Proxy configuration
        if (httpEndpoint.getHttpProxy() != null) {
            httpEndpoint.getHttpProxy().setHost(convert(httpEndpoint.getHttpProxy().getHost()));
            httpEndpoint.getHttpProxy().setUsername(convert(httpEndpoint.getHttpProxy().getUsername()));
            httpEndpoint.getHttpProxy().setPassword(convert(httpEndpoint.getHttpProxy().getPassword()));
        }

        // Default HTTP headers
        if (httpEndpoint.getHeaders() != null && !httpEndpoint.getHeaders().isEmpty()) {
            httpEndpoint.getHeaders().replaceAll((key, value) -> convert(value));
        }

        return httpEndpoint;
    }

    private String convert(String value) {
        if (value != null && !value.isEmpty()) {
            return templateEngine.convert(value);
        }

        return value;
    }

    @Override
    protected ManagedEndpoint create0(io.gravitee.definition.model.Endpoint endpoint) {
        URL uri = getURL(endpoint.getTarget());

        if (uri.getPath().isEmpty()) {
            logger.debug("Endpoint target URL is malformed for endpoint [{} - {}]. Set default path to '/'",
                    endpoint.getName(), endpoint.getTarget());
            endpoint.setTarget(endpoint.getTarget() + '/');
        }

        Connector connector = create(endpoint);

        applicationContext.getAutowireCapableBeanFactory().autowireBean(connector);

        return new ManagedEndpoint(endpoint, connector);
    }

    protected abstract Connector create(io.gravitee.definition.model.Endpoint endpoint);

    /**
     * Dummy {@link URLStreamHandler} implementation to avoid unknown protocol issue with default implementation
     * (which knows how to handle only http and https protocol).
     */
    private final URLStreamHandler URL_HANDLER = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return null;
        }
    };

    private URL getURL(String target) {
        if (target != null) {
            try {
                URL uri = new URL(null, target, URL_HANDLER);
                Objects.requireNonNull(uri.getProtocol(), "no null scheme accepted");
                Objects.requireNonNull(uri.getHost(), "no null host accepted");
                return uri;
            } catch (MalformedURLException e) {
                logger.error("HTTP endpoint target URL is malformed", e);
                throw new IllegalStateException("HTTP endpoint target URI is malformed: " + target);
            }
        }
        logger.error("HTTP endpoint target URL is null");
        throw new IllegalStateException("HTTP endpoint target URI is null");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}