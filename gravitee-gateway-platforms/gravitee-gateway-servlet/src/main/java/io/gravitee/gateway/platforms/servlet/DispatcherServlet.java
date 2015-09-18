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
package io.gravitee.gateway.platforms.servlet;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.Reactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class DispatcherServlet extends HttpServlet {

	private final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);

	protected void handle(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		logger.debug("Receive request {}", req);
		
		// Initialize async processing.
		final AsyncContext asyncContext = req.startAsync();
		// We do not timeout the continuation, but the proxy request
		asyncContext.setTimeout(0);

        getReactor()
                .process(RequestBuilder.from(req), RequestBuilder.from(resp), result -> handleResult(req, resp, result));
	}

    private void handleResult(final HttpServletRequest req, final HttpServletResponse resp, Response response) {
        logger.debug("Return response: {}", response);

        writeResponse(resp, response);


        try {
			/*
            final ServletOutputStream outputStream = resp.getOutputStream();
			ByteArrayOutputStream baos = (ByteArrayOutputStream) response.outputStream();
			baos.writeTo(outputStream);
		*/

			resp.flushBuffer();
        } catch (final IOException e) {
            logger.error("Error while handling proxy request", e);
        }

	    req.getAsyncContext().complete();
    }

    private void handleError(final HttpServletRequest req, final HttpServletResponse resp, Throwable throwable) {
        logger.error("An error occurs: ", throwable);

        req.getAsyncContext().complete();
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
		response.setStatus(proxyResponse.status());

		HttpHeaders headers = proxyResponse.headers();
		for (Map.Entry<String, List<String>> header : headers.entrySet()) {
			header.getValue().forEach(headerValue -> response.addHeader(header.getKey(), headerValue));
		}
	}

    protected abstract Reactor getReactor();
}
