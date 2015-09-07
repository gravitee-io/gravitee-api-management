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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.gateway.core.service.AbstractService;
import io.gravitee.reporters.access.file.config.Config;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@SuppressWarnings("rawtypes")
public class AccessLogReporter extends AbstractService implements Reporter {

	@Autowired
	private Config config;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogReporter.class);

	private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(RFC_3339_DATE_FORMAT);

	// A Dummy Container Date Object is used to format the date
	private Date date = new Date();

	private static ThreadLocal<StringBuilder> buffers = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder(256);
		}
	};
	
	private transient OutputStream _out;

	private transient Writer _writer;


	public void write(String accessLog) throws IOException {
		synchronized (this) {
			if (_writer == null) {
				return;
			}

			_writer.write(accessLog);
			_writer.write(System.lineSeparator());
			_writer.flush();
		}
	}

	protected String format(Request request, Response response) {
		StringBuilder buf = buffers.get();
		buf.setLength(0);

		long startTime = (long) request.date().getTime();

		// buf.append(request.getServerName());
		// buf.append(' ');
		//buf.append(request.getRemoteAddr());
		//buf.append(' ');

		buf.append('[');

		date.setTime(startTime);
		buf.append(dateFormatter.format(date));

		buf.append("] \"");
		buf.append(request.method());
		buf.append(' ');
		buf.append(request.uri());
		//buf.append(' ');
		//buf.append(request.protocol());
		buf.append("\" ");

		int status = response.status();
		if (status <= 0)
			status = 404;
		buf.append((char) ('0' + ((status / 100) % 10)));
		buf.append((char) ('0' + ((status / 10) % 10)));
		buf.append((char) ('0' + (status % 10)));

		long responseLength = getLongContentLength(response);
		if (responseLength >= 0) {
			buf.append(' ');
			if (responseLength > 99999) {
				buf.append(responseLength);
			} else {
				if (responseLength > 9999)
					buf.append((char) ('0' + ((responseLength / 10000) % 10)));
				if (responseLength > 999)
					buf.append((char) ('0' + ((responseLength / 1000) % 10)));
				if (responseLength > 99)
					buf.append((char) ('0' + ((responseLength / 100) % 10)));
				if (responseLength > 9)
					buf.append((char) ('0' + ((responseLength / 10) % 10)));
				buf.append((char) ('0' + (responseLength) % 10));
			}
			buf.append(' ');
		} else {
			// Send -1 in case of no content-length
			buf.append(" -1 ");
		}

//		final String apiName = api == null ? "-" : api.getName();
//		buf.append('"');
//		buf.append(apiName);
//		buf.append("\" ");

		String apiKey = request.id();
		if (apiKey == null) {
			apiKey = "-";
		}
		buf.append('"');
		buf.append(apiKey);
		buf.append("\" ");

		

		return buf.toString();
	}

	public String getResponseTime(Response response){
		// TODO add response missing information
		String responseTime = null;
		if (responseTime != null && !responseTime.isEmpty()) {
			return responseTime;
		}
		return "-1";
	}
	
	public long getLongContentLength(Response response) {
		String contentLength = response.headers().get("Content-Length");
		if (contentLength != null && !contentLength.isEmpty()) {
			return Integer.parseInt(contentLength);
		}

		return -1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void doStart() throws Exception {
		
		String filename = config.getFilename();
		
		if (filename != null) {
			_out = new RolloverFileOutputStream(filename);
			LOGGER.info("Opened " + filename);
		}

		synchronized (this) {
			_writer = new OutputStreamWriter(_out);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void doStop() throws Exception {
		synchronized (this) {
			try {
				if (_writer != null)
					_writer.flush();
			} catch (IOException ioe) {
				LOGGER.error("", ioe);
			}
			if (_out != null)
				try {
					_out.close();
				} catch (IOException ioe) {
					LOGGER.error("", ioe);
				}

			_out = null;
			_writer = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void report(Request request, Response response) {
		String log = format(request, response);
		try {
			write(log);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
