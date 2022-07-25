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
package io.gravitee.gateway.env;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpRequestTimeoutConfiguration {

    public HttpRequestTimeoutConfiguration(long httpRequestTimeout, Long httpRequestTimeoutGraceDelay) {
        this.httpRequestTimeout = httpRequestTimeout;
        this.httpRequestTimeoutGraceDelay = httpRequestTimeoutGraceDelay;
    }

    private long httpRequestTimeout;
    private long httpRequestTimeoutGraceDelay;

    public long getHttpRequestTimeout() {
        return httpRequestTimeout;
    }

    public void setHttpRequestTimeout(long httpRequestTimeout) {
        this.httpRequestTimeout = httpRequestTimeout;
    }

    public long getHttpRequestTimeoutGraceDelay() {
        return httpRequestTimeoutGraceDelay;
    }

    public void setHttpRequestTimeoutGraceDelay(long httpRequestTimeoutGraceDelay) {
        this.httpRequestTimeoutGraceDelay = httpRequestTimeoutGraceDelay;
    }
}
