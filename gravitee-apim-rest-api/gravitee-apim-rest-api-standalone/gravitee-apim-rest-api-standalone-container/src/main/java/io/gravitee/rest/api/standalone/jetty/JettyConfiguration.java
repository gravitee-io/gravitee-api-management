/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.standalone.jetty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@NoArgsConstructor
public class JettyConfiguration {

    @Value("${jetty.host:0.0.0.0}")
    private String httpHost;

    @Value("${jetty.port:8083}")
    private int httpPort;

    @Value("${jetty.idleTimeout:30000}")
    private int idleTimeout;

    @Value("${jetty.acceptors:-1}")
    private int acceptors;

    @Value("${jetty.selectors:-1}")
    private int selectors;

    @Value("${jetty.pool.minThreads:10}")
    private int poolMinThreads;

    @Value("${jetty.pool.maxThreads:200}")
    private int poolMaxThreads;

    @Value("${jetty.pool.idleTimeout:60000}")
    private int poolIdleTimeout;

    @Value("${jetty.pool.queueSize:6000}")
    private int poolQueueSize;

    @Value("${jetty.jmx:false}")
    private boolean jmxEnabled;

    @Value("${jetty.statistics:false}")
    private boolean statisticsEnabled;

    @Value("${jetty.accesslog.enabled:true}")
    private boolean accessLogEnabled;

    @Value("${jetty.accesslog.path:${gravitee.home}/logs/gravitee_accesslog_yyyy_mm_dd.log}")
    private String accessLogPath;

    @Value("${jetty.secured:false}")
    private boolean secured;

    @Value("${jetty.ssl.keystore.path:#{null}}")
    private String keyStorePath;

    @Value("${jetty.ssl.keystore.type:#{null}}")
    private String keyStoreType;

    @Value("${jetty.ssl.keystore.password:#{null}}")
    private String keyStorePassword;

    @Value("${jetty.ssl.truststore.path:#{null}}")
    private String trustStorePath;

    @Value("${jetty.ssl.truststore.type:#{null}}")
    private String trustStoreType;

    @Value("${jetty.ssl.truststore.password:#{null}}")
    private String trustStorePassword;

    @Value("${jetty.http.maxOutputBufferSize:32768}")
    private int maxOutputBufferSize;

    @Value("${jetty.http.maxRequestHeaderSize:8192}")
    private int maxRequestHeaderSize;

    @Value("${jetty.http.maxResponseHeaderSize:8192}")
    private int maxResponseHeaderSize;

    @Value("${jetty.http.sendServerVersion:false}")
    private boolean sendServerVersion;

    @Value("${jetty.http.sendDateHeader:false}")
    private boolean sendDateHeader;
}
