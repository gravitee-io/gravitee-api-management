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
package io.gravitee.gateway.reactor.handler;

import java.util.Collection;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpAcceptorFactory {

    private final boolean allowOverlappingApiContexts;

    public HttpAcceptorFactory(boolean allowOverlappingApiContexts) {
        this.allowOverlappingApiContexts = allowOverlappingApiContexts;
    }

    public HttpAcceptor create(String host, String path, ReactorHandler reactor, Collection<String> serverIds) {
        return allowOverlappingApiContexts
            ? new OverlappingHttpAcceptor(host, path, reactor, serverIds)
            : new DefaultHttpAcceptor(host, path, reactor, serverIds);
    }
}
