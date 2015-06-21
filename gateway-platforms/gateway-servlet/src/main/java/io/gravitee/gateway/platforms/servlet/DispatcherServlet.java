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

import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.Reactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

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
public abstract class DispatcherServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherServlet.class);

	protected void handle(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		// Initialize async processing.
		final AsyncContext asyncContext = req.startAsync();
		// We do not timeout the continuation, but the proxy request
		asyncContext.setTimeout(0);

        Observable<Response> response = getReactor().process(RequestBuilder.from(req));
        response.subscribe(new Action1<Response>() {
			@Override
			public void call(Response response) {
				LOGGER.debug("Return response: {}", response);

				writeResponse(resp, response);

				try {
					final ServletOutputStream outputStream = resp.getOutputStream();
					byte[] buffer = response.content();
					outputStream.write(buffer, 0, buffer.length);

					resp.flushBuffer();
				} catch (final IOException e) {
					LOGGER.error("Error while handling proxy request", e);
				}

				asyncContext.complete();
			}
		}, new Action1<Throwable>() {
			@Override
			public void call(Throwable throwable) {
				LOGGER.error("An error occurs: ", throwable);
				asyncContext.complete();
			}
		});
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

		Map<String, String> headers = proxyResponse.headers();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String hname = entry.getKey();
			String hval = entry.getValue();
            response.setHeader(hname, hval);
		}
	}

    protected abstract Reactor<Observable<Response>> getReactor();
}
