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
package io.gravitee.gateway.platforms.jetty.bootstrap;

import io.gravitee.gateway.api.Node;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.platforms.jetty.context.JettyPlatformContext;
import io.gravitee.gateway.platforms.jetty.node.JettyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gravitee.gateway.platforms.jetty.JettyEmbeddedContainer;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Bootstrap {

	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

	public static void main(String[] args) {
		final Node node = new JettyNode();

		try {
			node.start();

			// Ajout d'un hook pour gérer un arrêt propre.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					LOGGER.info("Shutting-down Gravitee Gateway...");
					try {
						node.stop();
					} catch (Exception ex) {
						LOGGER.error("Unable to stop Gravitee Gateway", ex);
					}
				}
			});
		} catch (Exception ex) {
			LOGGER.error("Unable to start Gravitee Gateway", ex);
		}
	}

	private Bootstrap() {
	}
}
