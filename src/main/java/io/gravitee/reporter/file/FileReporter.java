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
package io.gravitee.reporter.file;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.metrics.Metrics;
import io.gravitee.gateway.api.reporter.MetricsReporter;
import io.gravitee.reporter.file.config.Config;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Write an access log to a file by using the following line format:
 *
 * <pre>
 *     [TIMESTAMP] (LOCAL_IP) REMOTE_IP API KEY METHOD PATH STATUS LENGTH TOTAL_RESPONSE_TIME
 * </pre>
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@SuppressWarnings("rawtypes")
public class FileReporter extends AbstractService implements MetricsReporter {

	@Autowired
	private Config config;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileReporter.class);

	private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(RFC_3339_DATE_FORMAT);

	private static final String NO_STRING_DATA_VALUE = "-";

	private static final String NO_INTEGER_DATA_VALUE = "-1";

	private static ThreadLocal<StringBuilder> buffers = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder(256);
		}
	};

	private transient OutputStream _out;

	private transient Writer _writer;

	private void write(String accessLog) throws IOException {
		synchronized (this) {
			if (_writer == null) {
				return;
			}

			_writer.write(accessLog);
			_writer.write(System.lineSeparator());
			_writer.flush();
		}
	}

	private String format(Metrics metrics) {
		StringBuilder buf = buffers.get();
		buf.setLength(0);

		// Append request timestamp
		buf.append('[');
		buf.append(dateFormatter.format(Date.from(metrics.getRequestTimestamp())));
		buf.append("] ");

		// Append local IP
		buf.append('(');
		buf.append(metrics.getRequestLocalAddress());
		buf.append(") ");

		// Append remote IP
		buf.append(metrics.getRequestRemoteAddress());
		buf.append(' ');

		// Append Api name
		String apiName = metrics.getApiName();
		if (apiName == null) {
			apiName = NO_STRING_DATA_VALUE;
		}

		buf.append(apiName);
		buf.append(' ');

		// Append key
		String apiKey = metrics.getApiKey();
		if (apiKey == null) {
			apiKey = NO_STRING_DATA_VALUE;
		}
		buf.append(apiKey);
		buf.append(' ');

		// Append request method and URI
		buf.append(metrics.getRequestHttpMethod());
		buf.append(' ');
		buf.append(metrics.getRequestPath());
		buf.append(' ');

		// Append response status
		int status = metrics.getResponseHttpStatus();
		if (status <= 0)
			status = 404;
		buf.append((char) ('0' + ((status / 100) % 10)));
		buf.append((char) ('0' + ((status / 10) % 10)));
		buf.append((char) ('0' + (status % 10)));
		buf.append(' ');

		// Append response length
		long responseLength = metrics.getResponseContentLength();
		if (responseLength >= 0) {
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
		} else {
			buf.append(NO_INTEGER_DATA_VALUE);
		}
		buf.append(' ');

		// Append total response time
		buf.append(metrics.getProxyResponseTimeMs());

		return buf.toString();
	}

	@Override
	public synchronized void doStart() throws Exception {
        String filename = config.getFilename();
        if (filename != null) {
            _out = new RolloverFileOutputStream(
                    filename,
					config.isAppend(),
					config.getRetainDays(),
                    TimeZone.getDefault(),
					config.getDateFormat(),
					config.getBackupFormat()
            );
            LOGGER.info("Opened rollover access log file " + filename);
        }

		synchronized (this) {
			_writer = new OutputStreamWriter(_out);
		}
	}
	
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

	@Override
	public void report(Metrics metrics) {
		try {
			write(format(metrics));
		} catch (IOException ioe) {
			LOGGER.error("", ioe);
		}
	}
}
