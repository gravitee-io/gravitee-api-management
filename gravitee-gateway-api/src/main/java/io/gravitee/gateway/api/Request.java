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
package io.gravitee.gateway.api;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a server-side HTTP request.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Request {

    String id();

    /**
     * @return the URI of the request.
     */
    String uri();

    /**
     * @return The path part of the uri.
     */
    String path();

    /**
     * @return the query parameters in the request
     */
    Map<String, String> parameters();

    /**
     * @return the headers in the request.
     */
    HttpHeaders headers();

    /**
     * @return the HTTP method for the request.
     */
    HttpMethod method();

    /**
     * @return the HTTP version of the request
     */
    HttpVersion version();

    /**
     * The timestamp for when this request was received.
     * Specifically, this is the timestamp of creation of the request object.
     *
     * @return the instant timestamp for the request.
     */
    Instant timestamp();

    /**
     * Retrieves the body of the request as binary data.
     *
     * @return an {@link InputStream} object containing the body of the request.
     */
    InputStream inputStream();

    /**
     * Returns the Internet Protocol (IP) address of the client or last proxy that sent the request.
     *
     * @return a <code>String</code> containing the IP address of the client that sent the request.
     */
    String remoteAddress();

    /**
     * Returns the Internet Protocol (IP) address of the interface on which the request was received.
     *
     * @return a <code>String</code> containing the IP address on which the request was received.
     */
    String localAddress();
}
