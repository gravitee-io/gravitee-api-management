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
package io.gravitee.gateway.handlers.api;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ApiContextHandlerFactory implements ReactorHandlerFactory<Api> {

    private final Logger logger = LoggerFactory.getLogger(ApiContextHandlerFactory.class);

    @Autowired
    private ApplicationContext gatewayApplicationContext;

    @Override
    public ReactorHandler create(Api api) {
        if (api.isEnabled()) {
            AbstractApplicationContext internalApplicationContext = createApplicationContext(api);
            ApiReactorHandler handler = internalApplicationContext.getBean(ApiReactorHandler.class);
            handler.setClassLoader(internalApplicationContext.getClassLoader());
            return handler;
        } else {
            logger.warn("Api is disabled !");
            return null;
        }
    }

    private AbstractApplicationContext createApplicationContext(Api api) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setParent(gatewayApplicationContext);
        context.setClassLoader(new ReactorHandlerClassLoader(gatewayApplicationContext.getClassLoader()));
        context.setEnvironment((ConfigurableEnvironment) gatewayApplicationContext.getEnvironment());

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setEnvironment(gatewayApplicationContext.getEnvironment());
        context.addBeanFactoryPostProcessor(configurer);

        context.getBeanFactory().registerSingleton("api", api);
        context.register(ApiHandlerConfiguration.class);
        context.setId("context-api-" + api.getName());
        context.refresh();

        return context;
    }

    private static class ReactorHandlerClassLoader extends URLClassLoader {

        public ReactorHandlerClassLoader(ClassLoader parent) {
            super(new URL[]{}, parent);
        }
    }
}
