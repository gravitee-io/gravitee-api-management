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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
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
    Map<String, String> headers();

    /**
     * @return the HTTP method for the request.
     */
    HttpMethod method();

    /**
     * @return the HTTP version of the request
     */
    HttpVersion version();

    /**
     * Returns the length, in bytes, of the request body and made available by the input stream, or -1 if the length is
     * not known.
     *
     * @return a long containing the length of the request body or -1L if the length is not known.
     */
    long contentLength();

    /**
     * Returns the MIME type of the body of the request, or <code>null</code> if the type is not known.
     *
     * @return a <code>String</code> containing the name of the MIME type of the request, or null if the type is not
     * known.
     */
    String contentType();

    /**
     * Returns the Request TimeStamp.
     *
     * @return The time that the request was received.
     */
    Date timestamp();

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
