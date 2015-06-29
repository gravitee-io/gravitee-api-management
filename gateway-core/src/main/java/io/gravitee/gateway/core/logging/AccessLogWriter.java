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
package io.gravitee.gateway.core.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access log
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class AccessLogWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AccessLogWriter.class);

    private PrintWriter out = null;

    private String path;
    private String httpMethod;
    private String apiName;
    private Integer responseSize;
    private Long requestDurationInMs;

    public AccessLogWriter() {
        try {
            this.out = new PrintWriter(new FileWriter(new File("/etc/gravitee.io/log/access.log"), true));
        } catch (final IOException e) {
            LOG.error("Error while trying to open access log file", e);
        }
    }

    public AccessLogWriter path(final String path) {
        this.path = path;
        return this;
    }

    public AccessLogWriter httpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public AccessLogWriter apiName(final String apiName) {
        this.apiName = apiName;
        return this;
    }

    public AccessLogWriter responseSize(final int responseSize) {
        this.responseSize = responseSize;
        return this;
    }

    public AccessLogWriter requestDuration(final long requestDuration) {
        this.requestDurationInMs = requestDuration;
        return this;
    }

    public void write() {
        final StringBuilder sb = new StringBuilder(SimpleDateFormat.getInstance().format(new Date()));
        if (httpMethod != null) {
            sb.append(" ").append(httpMethod);
        }
        if (path != null) {
            sb.append(" ").append(path);
        }
        if (responseSize != null) {
            sb.append(" ").append(responseSize).append(" bytes");
        }
        if (requestDurationInMs != null) {
            sb.append(" ").append(requestDurationInMs).append(" ms");
        }
        if (apiName != null) {
            sb.append(" ").append(apiName);
        }
        LOG.debug(sb.toString());
        if (out != null) {
            out.println(sb.toString());
            out.flush();
        }
    }
}
