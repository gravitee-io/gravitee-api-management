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

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DispatcherServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherServlet.class);

	protected void handle(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		LOGGER.info("Accept request", req);

		// Initialize async processing.
		final AsyncContext asyncContext = req.startAsync();
		// We do not timeout the continuation, but the proxy request
		asyncContext.setTimeout(0);

		DefaultReactor reactor = new DefaultReactor();
		Processor processor = reactor.handle(RequestBuilder.from(req), new AsyncHandler<Response>() {
			@Override public void handle(Response result) {
				writeResponse(resp, result);
				
				asyncContext.complete();
			}
		});

		processor.process();
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

	protected void writeResponse(HttpServletResponse response, Response proxyResponse) {
		response.setStatus(proxyResponse.getStatus());

		Map<String, String> headers = proxyResponse.getHeaders();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String hname = entry.getKey();
			String hval = entry.getValue();
			response.setHeader(hname, hval);
		}
	}
}
