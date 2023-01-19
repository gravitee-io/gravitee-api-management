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
package io.gravitee.apim.gateway.tests.sdk.protocolhandlers.grpc;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * GRPC protocol is not supported by {@link URL} by default.
 * This class simply allow to support GRPC for URL parsing only inside the testing sdk.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GrpcConnection extends URLConnection {

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    protected GrpcConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        connected = true;
    }
}
