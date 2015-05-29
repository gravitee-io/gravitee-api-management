package io.gravitee.gateway.core.impl;

import io.gravitee.gateway.core.Processor;
import io.gravitee.gateway.http.Request;
import io.gravitee.gateway.http.Response;
import io.gravitee.gateway.core.AsyncHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestProcessor implements Processor {

	private final Request request;

	private final AsyncHandler<Response> responseHandler;

	public RequestProcessor(final Request request, final AsyncHandler<Response> responseHandler) {
		this.request = request;
		this.responseHandler = responseHandler;
	}

	@Override public void process() {
		try {
			HttpClient client = createHttpClient();
			org.eclipse.jetty.client.api.Request request = client.newRequest("target url here");

			request.content(new ContentProvider() {
				@Override public long getLength() {
					return 0;
				}

				@Override public Iterator<ByteBuffer> iterator() {
					return null;
				}
			});
		} catch (Exception ex) {

		}
	}

	protected HttpClient createHttpClient() throws Exception {
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

		client.start();

		// Content must not be decoded, otherwise the client gets confused
		client.getContentDecoderFactories().clear();

		return client;
	}

}
