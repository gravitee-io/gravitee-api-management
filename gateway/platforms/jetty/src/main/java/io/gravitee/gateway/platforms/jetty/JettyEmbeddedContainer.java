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
package io.gravitee.gateway.platforms.jetty;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.core.Platform;
import io.gravitee.gateway.core.Reactor;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyEmbeddedContainer extends AbstractLifecycleComponent<JettyEmbeddedContainer> implements Platform {

	protected final static String REGEX_SERVLET_MAPPING_ALL = "/*";

    @Autowired
    private Reactor reactor;

	private Server server;

	public String name() {
		//TODO: Get Jetty information somewhere from Jetty artifact
		return "Jetty"; //$NON-NLS-1$
	}

	@Override
	protected void doStart() throws Exception {
		server = new Server(8082);
		Handler rootHandler = createHandler();

		server.setHandler(rootHandler);
		server.start();
	}

	@Override
	protected void doStop() throws Exception {
		server.stop();
	}

	private Handler createHandler() {
		ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		handler.setAttribute(JettyDispatcherServlet.GRAVITEE_REACTOR_ATTRIBUTE, reactor);
		handler.setContextPath("/");

		ServletHolder dispatchServletHolder = new ServletHolder(JettyDispatcherServlet.class);
		handler.addServlet(dispatchServletHolder, REGEX_SERVLET_MAPPING_ALL);

		return handler;
	}
}
