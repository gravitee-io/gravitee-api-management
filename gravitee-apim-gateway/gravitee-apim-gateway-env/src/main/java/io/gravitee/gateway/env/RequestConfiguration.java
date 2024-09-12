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
package io.gravitee.gateway.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class RequestConfiguration {

    public static final Logger log = LoggerFactory.getLogger(RequestConfiguration.class);

    @Bean
    public RequestTimeoutConfiguration httpRequestTimeoutConfiguration(
        @Value("${http.requestTimeout:#{null}}") Long httpRequestTimeout,
        @Value("${http.requestTimeoutGraceDelay:30}") long httpRequestTimeoutGraceDelay
    ) {
        if (httpRequestTimeout == null) {
            log.warn("Http request timeout cannot be unset. Setting it to default value: 30_000 ms");
            httpRequestTimeout = 30_000L;
        } else if (httpRequestTimeout <= 0) {
            log.warn("A proper timeout (greater than 0) should be set in order to avoid unclose connection, suggested value is 30_000 ms");
        }
        return new RequestTimeoutConfiguration(httpRequestTimeout, httpRequestTimeoutGraceDelay);
    }

    @Bean
    public RequestClientAuthConfiguration httpRequestClientAuthConfiguration(
        @Value("${http.ssl.clientAuthHeader.name:#{null}}") String headerName
    ) {
        return new RequestClientAuthConfiguration(headerName);
    }
}
