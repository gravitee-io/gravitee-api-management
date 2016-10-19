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
package io.gravitee.management.standalone;

import java.io.File;

import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import io.gravitee.common.node.Node;
import io.gravitee.management.rest.spring.PropertiesConfiguration;
import io.gravitee.management.standalone.spring.StandaloneConfiguration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Container {

    private final Node node;
    private ConfigurableApplicationContext ctx;
    private boolean stopped = false;

    public Container() {
        initialize();

        // Get a reference to the node
        node = ctx.getBean(Node.class);
    }

    private void initialize() {
        initializeEnvironment();
        initializeLogging();
        initializeContext();
    }

    private void initializeEnvironment() {
        // Set system properties if needed
        String graviteeConfiguration = System.getProperty(PropertiesConfiguration.GRAVITEE_CONFIGURATION);
        if (graviteeConfiguration == null || graviteeConfiguration.isEmpty()) {
            String graviteeHome = System.getProperty("gravitee.home");
            System.setProperty(PropertiesConfiguration.GRAVITEE_CONFIGURATION,
                    graviteeHome + File.separator + "config" + File.separator + "gravitee.yml");
        }
    }

    private void initializeLogging() {
        String graviteeHome = System.getProperty("gravitee.home");
        String logbackConfiguration = graviteeHome + File.separator + "config" + File.separator + "logback.xml";
        File logbackConfigurationfile = new File(logbackConfiguration);

        // If logback configuration available, load it, else, load default logback configuration
        if (logbackConfigurationfile.exists()) {
            System.setProperty("logback.configurationFile", logbackConfigurationfile.getAbsolutePath());
            StaticLoggerBinder loggerBinder = StaticLoggerBinder.getSingleton();
            LoggerContext loggerContext = (LoggerContext) loggerBinder.getLoggerFactory();
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            try {
                configurator.doConfigure(logbackConfigurationfile);
            } catch( JoranException e ) {
                e.printStackTrace();
            }

            // Internal status data is printed in case of warnings or errors.
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
        }
    }

    private void initializeContext() {
        LoggerFactory.getLogger(Container.class).info("Initializing Gravitee Management API context...");
        ctx = new AnnotationConfigApplicationContext();
        ((AnnotationConfigApplicationContext)ctx).register(StandaloneConfiguration.class);
        ctx.refresh();
    }

    /**
     * Start a new GraviteeIO Management Standalone node.
     */
    public void start() {
        LoggerFactory.getLogger(Container.class).info("Start Gravitee Management API...");

        try {
            node.start();

            // Register shutdown hook
            Thread shutdownHook = new ContainerShutdownHook();
            shutdownHook.setName("gravitee-finalizer");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (Exception ex) {
            LoggerFactory.getLogger(Container.class).error("An unexpected error occurs while starting Gravitee Management API", ex);
            stop();
        }
    }

    /**
     * Stop an existing GraviteeIO Management Standalone node.
     */
    public void stop() {
        if (! stopped) {
            LoggerFactory.getLogger(Container.class).info("Shutting-down Gravitee Management API...");

            try {
                node.stop();
            } catch (Exception ex) {
                LoggerFactory.getLogger(Container.class).error("Unexpected error", ex);
            } finally {
                ctx.close();
                stopped = true;
            }
        }
    }

    private class ContainerShutdownHook extends Thread {

        @Override
        public void run() {
            if (node != null) {
                Container.this.stop();
            }
        }
    }

    public static void main(String[] args) {
        // If you want to run Gravitee standalone from your IDE, please do not forget
        // to specify -Dgravitee.home=/path/to/gravitee/home in order to make it works.
        Container container = new Container();
        container.start();
    }
}
