package io.gravitee.gateway.platforms.jetty.bootstrap;

import io.gravitee.gateway.platforms.jetty.JettyEmbeddedContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Bootstrap {

	private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

	public static void main(String[] args) {
		final JettyEmbeddedContainer container = new JettyEmbeddedContainer();

		try {
			container.start();

			// Ajout d'un hook pour gérer un arrêt propre.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					LOGGER.info("Shutting-down Gravity Gateway...");
					try {
						container.stop();
					} catch (Exception ex) {
						LOGGER.error("Unable to stop Gravity Gateway", ex);
					}
				}
			});
		} catch (Exception ex) {
			LOGGER.error("Unable to start Gravity Gateway", ex);
		}
	}
}
