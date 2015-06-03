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

import io.gravitee.gateway.core.PlatformContext;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.Registry;
import io.gravitee.gateway.core.http.Response;
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
                writeResponse(resp, response);


                try {
                    final ServletOutputStream outputStream = resp.getOutputStream();
                    byte [] buffer = response.getContent();
                    outputStream.write(buffer, 0, buffer.length);

                    resp.flushBuffer();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
		response.setStatus(proxyResponse.getStatus());

		Map<String, String> headers = proxyResponse.getHeaders();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String hname = entry.getKey();
			String hval = entry.getValue();
            response.setHeader(hname, hval);
		}
	}

    private Registry getRegistry() {
        return getPlatformContext().getRegistry();
    }

    private Reactor getReactor() {
        return getPlatformContext().getReactor();
    }

    protected abstract PlatformContext getPlatformContext();
}
