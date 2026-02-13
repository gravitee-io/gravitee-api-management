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
package io.gravitee.rest.api.standalone.jetty;

import io.gravitee.apim.rest.api.automation.GraviteeAutomationApplication;
import io.gravitee.apim.rest.api.automation.security.SecurityAutomationConfiguration;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.plugin.console.ConsoleExtensionApplication;
import io.gravitee.plugin.console.ConsoleExtensionManager;
import io.gravitee.plugin.console.ConsoleExtensionServletFactory;
import io.gravitee.rest.api.management.rest.resource.GraviteeManagementApplication;
import io.gravitee.rest.api.management.security.SecurityManagementConfiguration;
import io.gravitee.rest.api.management.v2.rest.GraviteeManagementV2Application;
import io.gravitee.rest.api.management.v2.security.SecurityManagementV2Configuration;
import io.gravitee.rest.api.portal.rest.resource.GraviteePortalApplication;
import io.gravitee.rest.api.portal.security.SecurityPortalConfiguration;
import io.gravitee.rest.api.standalone.jetty.handler.NoContentOutputErrorHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class JettyEmbeddedContainer extends AbstractLifecycleComponent<JettyEmbeddedContainer> implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(JettyEmbeddedContainer.class);

    private final JettyServerFactory jettyServerFactory;

    private Server server;
    private ApplicationContext applicationContext;

    @Value("${http.api.management.enabled:true}")
    private boolean startManagementAPI;

    @Value("${http.api.management.entrypoint:${http.api.entrypoint:/}management}")
    private String managementEntrypoint;

    @Value("${http.api.automation.enabled:true}")
    private boolean startAutomationAPI;

    @Value("${http.api.automation.entrypoint:${http.api.entrypoint:/}automation}")
    private String automationEntrypoint;

    @Value("${http.api.portal.enabled:true}")
    private boolean startPortalAPI;

    @Value("${http.api.portal.entrypoint:${http.api.entrypoint:/}portal}")
    private String portalEntrypoint;

    public JettyEmbeddedContainer(JettyServerFactory jettyServerFactory) {
        this.jettyServerFactory = jettyServerFactory;
    }

    @Override
    protected void doStart() throws Exception {
        server = Objects.requireNonNull(jettyServerFactory.getObject());

        Request.Handler noContentHandler = new NoContentOutputErrorHandler();
        // This part is needed to avoid WARN while starting container.

        server.setErrorHandler(noContentHandler);

        // Spring configuration
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "basic");

        List<ServletContextHandler> contexts = new ArrayList<>();

        if (startPortalAPI) {
            // REST configuration for Portal API
            ServletContextHandler portalContextHandler = configureAPI(
                portalEntrypoint,
                GraviteePortalApplication.class.getName(),
                SecurityPortalConfiguration.class
            );
            contexts.add(portalContextHandler);
        }

        if (startAutomationAPI) {
            // REST configuration for Automation API
            ServletContextHandler automationContextHandler = configureAPI(
                automationEntrypoint,
                GraviteeAutomationApplication.class.getName(),
                SecurityAutomationConfiguration.class
            );
            contexts.add(automationContextHandler);
        }

        if (startManagementAPI) {
            // REST configuration for Management API
            ServletContextHandler managementContextHandler = configureAPI(
                managementEntrypoint,
                GraviteeManagementApplication.class.getName(),
                SecurityManagementConfiguration.class
            );
            contexts.add(managementContextHandler);

            // REST configuration for Management API - V2
            ServletContextHandler managementV2ContextHandler = configureAPI(
                managementEntrypoint + "/v2",
                GraviteeManagementV2Application.class.getName(),
                SecurityManagementV2Configuration.class
            );
            contexts.add(managementV2ContextHandler);

            // Configuration for Console extensions.
            contexts.add(
                configureAPI(
                    managementEntrypoint + "/v2/extensions",
                    ConsoleExtensionApplication.class.getName(),
                    SecurityManagementV2Configuration.class
                )
            );

            // Dynamic servlet extensions (e.g. MCP server)
            ConsoleExtensionManager cem = applicationContext.getBean(ConsoleExtensionManager.class);
            for (var entry : cem.getServletFactories()) {
                ConsoleExtensionServletFactory factory = entry.getValue();
                String contextPath = managementEntrypoint + factory.getServletPath();
                logger.info("Mounting console extension servlet '{}' at {}", entry.getKey(), contextPath);
                contexts.add(configureServletExtension(contextPath, factory));
            }
        }

        if (contexts.isEmpty()) {
            throw new IllegalStateException("At least one API should be enabled");
        }

        server.setHandler(new ContextHandlerCollection(contexts.toArray(new ServletContextHandler[0])));

        // start the server
        server.start();
    }

    private ServletContextHandler configureAPI(
        String apiContextPath,
        String applicationName,
        Class<? extends GlobalAuthenticationConfigurerAdapter> securityConfigurationClass
    ) {
        final ServletContextHandler childContext = new ServletContextHandler(apiContextPath, ServletContextHandler.SESSIONS);

        final ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
        servletHolder.setInitParameter("jakarta.ws.rs.Application", applicationName);
        servletHolder.setInitOrder(0);
        childContext.addServlet(servletHolder, "/*");

        AnnotationConfigWebApplicationContext webApplicationContext = new AnnotationConfigWebApplicationContext();
        webApplicationContext.register(securityConfigurationClass);

        webApplicationContext.setEnvironment((ConfigurableEnvironment) applicationContext.getEnvironment());
        webApplicationContext.setParent(applicationContext);

        childContext.addEventListener(new ContextLoaderListener(webApplicationContext));

        // Spring Security filter
        childContext.addFilter(
            new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")),
            "/*",
            EnumSet.allOf(DispatcherType.class)
        );
        return childContext;
    }

    private ServletContextHandler configureServletExtension(String contextPath, ConsoleExtensionServletFactory factory) {
        final ServletContextHandler extContext = new ServletContextHandler(contextPath, ServletContextHandler.NO_SESSIONS);

        // Create servlet via the factory
        HttpServlet servlet = factory.createServlet(applicationContext);
        ServletHolder servletHolder = new ServletHolder("extension", servlet);
        servletHolder.setAsyncSupported(true);
        extContext.addServlet(servletHolder, "/*");

        // Create child Spring context for the extension
        AnnotationConfigWebApplicationContext webApplicationContext = new AnnotationConfigWebApplicationContext();
        Class<?>[] configClasses = factory.getSpringConfigClasses();
        if (configClasses != null && configClasses.length > 0) {
            webApplicationContext.register(configClasses);
        }
        webApplicationContext.setClassLoader(factory.getClass().getClassLoader());
        webApplicationContext.setEnvironment((ConfigurableEnvironment) applicationContext.getEnvironment());
        webApplicationContext.setParent(applicationContext);

        // Register servlet as a singleton bean so Spring config classes can use it
        webApplicationContext.addBeanFactoryPostProcessor(beanFactory ->
            beanFactory.registerSingleton("mcpTransportProvider", servlet)
        );

        extContext.addEventListener(new ContextLoaderListener(webApplicationContext));

        // Spring Security filter
        extContext.addFilter(
            new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")),
            "/*",
            EnumSet.allOf(DispatcherType.class)
        );

        return extContext;
    }

    @Override
    protected void doStop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
