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
package io.gravitee.repository.bridge.server.version;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.util.Version;
import io.gravitee.repository.bridge.server.utils.VersionUtils;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This handler is used to check the version of the bridge client from the user-agent.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VersionHandler implements Handler<RoutingContext> {

    private static final VersionUtils.Version NODE_VERSION = VersionUtils.parse(Version.RUNTIME_VERSION.MAJOR_VERSION);

    private final static String USER_AGENT_PREFIX = "gio-client-bridge/";

    @Override
    public void handle(RoutingContext ctx) {
        String userAgentHeader = ctx.request().getHeader(HttpHeaders.USER_AGENT);

        if (userAgentHeader != null && userAgentHeader.startsWith(USER_AGENT_PREFIX)) {
            String sVersion = userAgentHeader.substring(USER_AGENT_PREFIX.length());
            VersionUtils.Version clientVersion = VersionUtils.parse(sVersion);

            // If the version is not valid or if it doesn't match the server, reject the call
            if (clientVersion == null || NODE_VERSION.major() != clientVersion.major()) {
                ctx.fail(new IllegalStateException("Version of the bridge client is invalid"));
            } else {
                ctx.next();
            }
        } else {
            // The call is probably not coming from a bridge client, let's continue processing
            ctx.next();
        }
    }
}
