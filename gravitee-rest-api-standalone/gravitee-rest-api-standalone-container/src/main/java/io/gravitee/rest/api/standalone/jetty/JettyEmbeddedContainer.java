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
package io.gravitee.rest.api.standalone.jetty;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.rest.api.management.rest.resource.GraviteeManagementApplication;
import io.gravitee.rest.api.management.security.SecurityManagementConfiguration;
import io.gravitee.rest.api.portal.rest.resource.GraviteePortalApplication;
import io.gravitee.rest.api.portal.security.SecurityPortalConfiguration;
import io.gravitee.rest.api.standalone.jetty.handler.NoContentOutputErrorHandler;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class JettyEmbeddedContainer extends AbstractLifecycleComponent<JettyEmbeddedContainer> implements ApplicationContextAware {

    @Autowired
    private Server server;

    private ApplicationContext applicationContext;

    @Value("${http.api.management.enabled:true}")
    private boolean startManagementAPI;

    @Value("${http.api.management.entrypoint:${http.api.entrypoint:/}management}")
    private String managementEntrypoint;
    
    @Value("${http.api.portal.enabled:true}")
    private boolean startPortalAPI;

    @Value("${http.api.portal.entrypoint:${http.api.entrypoint:/}portal}")
    private String portalEntrypoint;

    @Override
    protected void doStart() throws Exception {
        AbstractHandler noContentHandler = new NoContentOutputErrorHandler();
        // This part is needed to avoid WARN while starting container.
        noContentHandler.setServer(server);
        server.addBean(noContentHandler);

        // Spring configuration
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "basic");

        List<ServletContextHandler> contexts = new ArrayList<>();
        
        if (startPortalAPI) {
            // REST configuration for Portal API
            ServletContextHandler portalContextHandler = configureAPI(portalEntrypoint, GraviteePortalApplication.class.getName(), SecurityPortalConfiguration.class);
            contexts.add(portalContextHandler);
        }
        
        if (startManagementAPI) {
            // REST configuration for Management API
            ServletContextHandler managementContextHandler = configureAPI(managementEntrypoint, GraviteeManagementApplication.class.getName(), SecurityManagementConfiguration.class);
            contexts.add(managementContextHandler);
        }
        
        if (contexts.isEmpty()) {
            throw new IllegalStateException("At least one API should be enabled");
        }

        server.setHandler(new ContextHandlerCollection(contexts.toArray(new ServletContextHandler[contexts.size()])));

        // start the server
        server.start();
    }

    protected ServletContextHandler configureAPI(String apiContextPath, String applicationName, Class<? extends GlobalAuthenticationConfigurerAdapter> securityConfigurationClass) {
        final ServletContextHandler childContext = new ServletContextHandler(server, apiContextPath, ServletContextHandler.SESSIONS);

        final ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
        servletHolder.setInitParameter("javax.ws.rs.Application", applicationName);
        servletHolder.setInitOrder(0);
        childContext.addServlet(servletHolder, "/*");

        AnnotationConfigWebApplicationContext webApplicationContext = new AnnotationConfigWebApplicationContext();
        webApplicationContext.register(securityConfigurationClass);

        webApplicationContext.setEnvironment((ConfigurableEnvironment) applicationContext.getEnvironment());
        webApplicationContext.setParent(applicationContext);

        childContext.addEventListener(new ContextLoaderListener(webApplicationContext));

        // Spring Security filter
        childContext.addFilter(new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")),"/*", EnumSet.allOf(DispatcherType.class));
        return childContext;


    }
    
    @Override
    protected void doStop() throws Exception {
        server.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}