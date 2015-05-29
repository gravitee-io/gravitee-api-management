package io.gravitee.gateway.platforms.servlet;

import io.gravitee.gateway.core.Processor;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.http.Response;
import io.gravitee.gateway.core.AsyncHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DispatcherServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherServlet.class);

	protected void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Initialize async processing.
	//	final AsyncContext context = req.startAsync();

		DefaultReactor reactor = new DefaultReactor();
		Processor processor = reactor.handle(RequestBuilder.from(req), new AsyncHandler<Response>() {
			@Override public void handle(Response result) {

			}
		});

		processor.process();

		HttpClient client = createHttpClient();
		client.

		LOGGER.info("Accept request", req);
	}

	protected HttpClient createHttpClient() throws ServletException {
		HttpClient client = new HttpClient();

		// Redirects must be proxied as is, not followed
		client.setFollowRedirects(false);

		// Must not store cookies, otherwise cookies of different clients will mix
		client.setCookieStore(new HttpCookieStore.Empty());

		QueuedThreadPool qtp = new QueuedThreadPool(2);

		qtp.setName("dispatcher");

		client.setExecutor(qtp);
		client.setIdleTimeout(30000);
		client.setRequestBufferSize(16384);
		client.setResponseBufferSize(163840);

		try {
			client.start();

			// Content must not be decoded, otherwise the client gets confused
			client.getContentDecoderFactories().clear();

			return client;
		} catch (Exception x) {
			throw new ServletException(x);
		}
	}

	@Override protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	@Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	@Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	@Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	@Override protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	@Override protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}

	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}
}
