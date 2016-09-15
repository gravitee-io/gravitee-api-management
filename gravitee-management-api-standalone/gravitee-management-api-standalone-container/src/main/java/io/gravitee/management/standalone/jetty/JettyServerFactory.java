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
package io.gravitee.management.standalone.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JettyServerFactory implements FactoryBean<Server> {

    @Autowired
    private JettyConfiguration jettyConfiguration;

    @Override
    public Server getObject() throws Exception {

        // Setup ThreadPool
        QueuedThreadPool threadPool = new QueuedThreadPool(
                jettyConfiguration.getPoolMaxThreads(),
                jettyConfiguration.getPoolMinThreads(),
                jettyConfiguration.getPoolIdleTimeout(),
                new ArrayBlockingQueue<Runnable>(jettyConfiguration.getPoolQueueSize())
        );
        threadPool.setName("gravitee-listener");

        Server server = new Server(threadPool);

        // Extra options
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);
        server.setStopAtShutdown(true);

        // Setup JMX
        if (jettyConfiguration.isJmxEnabled()) {
            MBeanContainer mbContainer = new MBeanContainer(
                    ManagementFactory.getPlatformMBeanServer());
            server.addBean(mbContainer);
        }

        // HTTP Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);

        // Setup Jetty HTTP Connector
        ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(httpConfig));
        http.setPort(jettyConfiguration.getHttpPort());
        http.setIdleTimeout(jettyConfiguration.getIdleTimeout());

        server.addConnector(http);

        // Setup Jetty statistics
        if (jettyConfiguration.isStatisticsEnabled()) {
            StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(server.getHandler());
            server.setHandler(stats);
        }

        if (jettyConfiguration.isAccessLogEnabled()) {
            AsyncNCSARequestLog requestLog = new AsyncNCSARequestLog(
                    jettyConfiguration.getAccessLogPath());
            requestLog.setRetainDays(90);
            requestLog.setExtended(true);
            requestLog.setLogLatency(true);
            requestLog.setLogTimeZone("GMT");

            server.setRequestLog(requestLog);
        }

        return server;
    }

    @Override
    public Class<?> getObjectType() {
        return Server.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
