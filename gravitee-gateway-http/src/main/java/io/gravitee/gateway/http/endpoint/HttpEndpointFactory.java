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

import io.gravitee.definition.model.EndpointType;
import io.gravitee.gateway.core.endpoint.factory.template.TemplateAwareEndpointFactory;
import io.gravitee.gateway.http.connector.VertxHttpClient;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class HttpEndpointFactory extends TemplateAwareEndpointFactory<io.gravitee.definition.model.endpoint.HttpEndpoint, HttpEndpoint>
        implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public boolean support(EndpointType endpointType) {
        return EndpointType.HTTP == endpointType;
    }

    @Override
    protected io.gravitee.definition.model.endpoint.HttpEndpoint resolve(io.gravitee.definition.model.endpoint.HttpEndpoint endpoint) {
        // HTTP endpoint configuration
        endpoint.setTarget(convert(endpoint.getTarget()));
        endpoint.setHostHeader(convert(endpoint.getHostHeader()));

        // HTTP Proxy configuration
        if (endpoint.getHttpProxy() != null) {
            endpoint.getHttpProxy().setHost(convert(endpoint.getHttpProxy().getHost()));
            endpoint.getHttpProxy().setUsername(convert(endpoint.getHttpProxy().getUsername()));
            endpoint.getHttpProxy().setPassword(convert(endpoint.getHttpProxy().getPassword()));
        }

        return endpoint;
    }

    @Override
    protected HttpEndpoint create0(io.gravitee.definition.model.endpoint.HttpEndpoint endpoint) {
        VertxHttpClient httpClient = new VertxHttpClient(endpoint);

        applicationContext.getAutowireCapableBeanFactory().autowireBean(httpClient);

        return new HttpEndpoint(endpoint, httpClient);
    }

    private String convert(String value) {
        if (value != null && ! value.isEmpty()) {
            return templateEngine.convert(value);
        }

        return value;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
