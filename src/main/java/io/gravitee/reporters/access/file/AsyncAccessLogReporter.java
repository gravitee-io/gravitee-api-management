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
package io.gravitee.reporters.access.file;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;

/**
 * Asynchronous AccessLogReporter
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AsyncAccessLogReporter extends AccessLogReporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncAccessLogReporter.class);

	private final BlockingQueue<String> queue = new BlockingArrayQueue<>(1024);

	private transient WriterThread thread;

	private boolean warnedFull;

	@Override
	public synchronized void doStart() throws Exception {
		super.doStart();
		LOGGER.info("Async AccessLog reporter is starting.");

		this.thread = new WriterThread();
		this.thread.start();
	}

	@Override
	public synchronized void doStop() throws Exception {
		LOGGER.info("Stop AccessLog reporter...");
		thread.terminate();
		thread.join();
		super.doStop();
		thread = null;
		LOGGER.info("Stop AccessLog reporter... DONE");
	}

	@Override
	public void report(Request request, Response response) {
		String log = format(request, response);
		if (!this.queue.offer(log)) {
			if (this.warnedFull) {
				// TODO: provide a programmatic overflow to disk feature
				LOGGER.warn("Accesslog Queue overflow !");
			}
			this.warnedFull = true;
		}
	}

	private class WriterThread extends Thread {
		private volatile boolean running = true;

		WriterThread() {
			this.setName("reporter-accesslog");
		}

		public void terminate() {
			running = false;
		}

		@Override
		public void run() {
			while (running) {
				try {
					String log = AsyncAccessLogReporter.this.queue.poll(10, TimeUnit.SECONDS);
					if (log != null) {
						AsyncAccessLogReporter.this.write(log);
					}

					while (!AsyncAccessLogReporter.this.queue.isEmpty()) {
						log = AsyncAccessLogReporter.this.queue.poll();
						if (log != null) {
							AsyncAccessLogReporter.this.write(log);
						}
					}
				} catch (IOException ioe) {
					LOGGER.error("", ioe);
				} catch (InterruptedException ie) {
					running = false;
				}
			}
		}
	}
}
