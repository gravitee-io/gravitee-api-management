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
package io.gravitee.gateway.reactive.debug.reactor.processor;

import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseRequest;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public class DebugInitProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugInitProcessor.class);

    @Override
    public String getId() {
        return "processor-debug-init";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            HttpBaseRequest request = ctx.request();
            String scheme = request.scheme();
            String originalHost = request.originalHost();
            String uri = request.uri();

            String originalUrl = scheme + "://" + originalHost + uri;
            LOGGER.debug("Original URL: {}", originalUrl);

            ctx.setAttribute(ContextAttributes.ATTR_REQUEST_ORIGINAL_URL, originalUrl);
        });
    }
}
