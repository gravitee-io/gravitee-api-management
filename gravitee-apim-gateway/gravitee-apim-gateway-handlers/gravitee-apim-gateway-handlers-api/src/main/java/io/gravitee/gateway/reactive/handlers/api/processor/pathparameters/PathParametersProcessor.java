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
package io.gravitee.gateway.reactive.handlers.api.processor.pathparameters;

import io.gravitee.gateway.handlers.api.processor.pathparameters.AbstractPathParametersExtractor;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParametersProcessor implements Processor {

    public static final String ID = "processor-path-parameters";

    private final AbstractPathParametersExtractor<?, ?, ?> extractor;

    public PathParametersProcessor(AbstractPathParametersExtractor<?, ?, ?> extractor) {
        this.extractor = extractor;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() ->
            extractor
                .extract(ctx.request().method().name(), ctx.request().pathInfo())
                .forEach((key, value) -> ctx.request().pathParameters().set(key, value))
        );
    }
}
