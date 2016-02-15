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
package io.gravitee.gateway.standalone.junit.stmt;

import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.utils.SocketUtils;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class ApiPublisherStatement extends Statement {

    private final Statement base;
    private final Description description;

    private List<Server> servers;

    public ApiPublisherStatement(Statement base, Description description) {
        this.base = base;
        this.description = description;
    }

    @Override
    public void evaluate() throws Throwable {
        startServer(description.getAnnotation(ApiConfiguration.class));

        try {
            base.evaluate();
        } finally {
            stopServer();
        }
    }

    private void startServer(ApiConfiguration apiConfiguration) throws Exception {
        // Creation of the Jetty server
        // === jetty.xml ===
        // Setup Threadpool
        QueuedThreadPool threadPool = new QueuedThreadPool(10);

        servers = new ArrayList<>(apiConfiguration.workers());
        for(int i = 0 ; i < apiConfiguration.workers() ; i++) {
            // Server
            Server server = new Server(threadPool);

            // HTTP Configuration
            HttpConfiguration http_config = new HttpConfiguration();
            http_config.setOutputBufferSize(32768);
            http_config.setRequestHeaderSize(8192);
            http_config.setResponseHeaderSize(8192);
            http_config.setSendServerVersion(true);
            http_config.setSendDateHeader(false);

            // === jetty-http.xml ===
            ServerConnector http = new ServerConnector(server,
                    new HttpConnectionFactory(http_config));
            http.setPort(SocketUtils.generateFreePort());
            http.setIdleTimeout(30000);
            server.addConnector(http);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            context.setContextPath(apiConfiguration.contextPath());
            ServletHolder servletHolder = new ServletHolder(apiConfiguration.servlet());
            servletHolder.setInitParameter("worker", "worker#" + i);

            context.addServlet(servletHolder, "/*");

            server.setHandler(context);
            server.start();
        }
    }

    private void stopServer() throws Exception {
        SocketUtils.clearUsedPorts();
        if (servers != null) {
            for(Server server : servers) {
                server.stop();
            }
        }
    }
}
