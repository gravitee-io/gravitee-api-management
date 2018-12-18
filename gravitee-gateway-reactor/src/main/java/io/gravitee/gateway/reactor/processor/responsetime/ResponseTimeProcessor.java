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
package io.gravitee.gateway.reactor.processor.responsetime;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTimeProcessor extends AbstractProcessor<ExecutionContext> {

    @Override
    public void handle(ExecutionContext context) {
        // Compute response-time and add it to the metrics
        long proxyResponseTimeInMs = System.currentTimeMillis() - context.request().metrics().timestamp().toEpochMilli();
        context.request().metrics().setStatus(context.response().status());
        context.request().metrics().setProxyResponseTimeMs(proxyResponseTimeInMs);
        context.request().metrics().setProxyLatencyMs(proxyResponseTimeInMs - context.request().metrics().getApiResponseTimeMs());

        // Push response to the next handler
        next.handle(context);
    }
}