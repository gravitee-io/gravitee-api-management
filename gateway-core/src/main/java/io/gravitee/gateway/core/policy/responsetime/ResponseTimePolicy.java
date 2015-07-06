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
package io.gravitee.gateway.core.policy.responsetime;

import io.gravitee.gateway.api.PolicyChain;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.policy.PolicyAdapter;
import io.gravitee.gateway.core.policy.annotations.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Policy(name = "response-time")
public class ResponseTimePolicy implements io.gravitee.gateway.api.Policy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseTimePolicy.class);

    private final static String X_GATEWAY_RESPONSETIME = "X-Gateway-ResponseTime";

    private long startTime;

    @Override
    public void onRequest(Request request, Response response, PolicyChain handler) {
        LOGGER.debug("Applying {} to request {}", description(), request.id());

        startTime = System.currentTimeMillis();

        handler.doNext(request, response);
    }

    @Override
    public void onResponse(Request request, Response response, PolicyChain handler) {
        long elapsed = System.currentTimeMillis() - startTime;

        response.headers().put(X_GATEWAY_RESPONSETIME, elapsed + "ms");
        handler.doNext(request, response);
    }

    @Override
    public String description() {
        return "Response Time Policy";
    }
}
