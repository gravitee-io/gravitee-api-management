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
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.endpoint.factory.template.TemplateAwareEndpointFactory;
import io.gravitee.gateway.http.connector.VertxHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class HttpEndpointFactory extends TemplateAwareEndpointFactory<io.gravitee.definition.model.endpoint.HttpEndpoint, HttpEndpoint>
        implements ApplicationContextAware {
    
	private static final String DICTIONARY_TEMPLATE_PROVIDER_CLASS_NAME = "io.gravitee.gateway.dictionary.DictionaryTemplateProvider";

    private final Logger logger = LoggerFactory.getLogger(HttpEndpointFactory.class);

    private ApplicationContext applicationContext;
    
    private TemplateVariableProvider dictionaryVariableProvider;

    @Override
    public boolean support(EndpointType endpointType) {
        return EndpointType.HTTP == endpointType;
    }

    @Override
    protected io.gravitee.definition.model.endpoint.HttpEndpoint resolve(io.gravitee.definition.model.endpoint.HttpEndpoint endpoint) {
        // HTTP endpoint configuration
        endpoint.setTarget(convert(endpoint.getTarget()));

        // HTTP Proxy configuration
        if (endpoint.getHttpProxy() != null) {
            endpoint.getHttpProxy().setHost(convert(endpoint.getHttpProxy().getHost()));
            endpoint.getHttpProxy().setUsername(convert(endpoint.getHttpProxy().getUsername()));
            endpoint.getHttpProxy().setPassword(convert(endpoint.getHttpProxy().getPassword()));
        }

        // Default HTTP headers
        if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
            endpoint.getHeaders().replaceAll((key, value) -> convert(value));
        }
        return endpoint;
    }

    @Override
    protected HttpEndpoint create0(io.gravitee.definition.model.endpoint.HttpEndpoint endpoint) {

        try {
            URL url = new URL(endpoint.getTarget());
            if (url.getPath().isEmpty()) {
                logger.warn("HTTP endpoint target URL is malformed for endpoint [{} - {}]. Set default path to '/'",
                        endpoint.getName(), endpoint.getTarget());
                endpoint.setTarget(endpoint.getTarget() + '/');
            }

            VertxHttpClient httpClient = new VertxHttpClient(endpoint);

            applicationContext.getAutowireCapableBeanFactory().autowireBean(httpClient);

            return new HttpEndpoint(endpoint, httpClient);
        } catch (MalformedURLException murle) {
            logger.error("HTTP endpoint target URL is malformed", murle);
            throw new IllegalStateException("HTTP endpoint target URL is malformed: " + endpoint.getTarget());
        }
    }

    private String convert(String value) {
        if (value != null && ! value.isEmpty()) {
        	if(isDictionaryTemplateVariableProviderAvailable()) {
        		dictionaryVariableProvider.provide(templateEngine.getTemplateContext());
        	}
            return templateEngine.convert(value);
        }

        return value;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private boolean isDictionaryTemplateVariableProviderAvailable() {
    	try {
			dictionaryVariableProvider = (TemplateVariableProvider) applicationContext.getBean(applicationContext.getClassLoader().loadClass(DICTIONARY_TEMPLATE_PROVIDER_CLASS_NAME));
			return dictionaryVariableProvider != null;
		} catch (BeansException | ClassNotFoundException e) {
			logger.warn("DictionaryTemplateProvider not found: Dictionary support for endpoint disabled. Reason: {}", e.toString());
			return false;
		}
    }
}
