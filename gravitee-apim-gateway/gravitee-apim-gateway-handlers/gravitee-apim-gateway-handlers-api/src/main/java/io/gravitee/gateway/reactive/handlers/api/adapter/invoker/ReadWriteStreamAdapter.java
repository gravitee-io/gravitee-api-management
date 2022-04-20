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
package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext;
import io.gravitee.gateway.reactive.api.context.sync.SyncRequest;
import io.gravitee.gateway.reactive.handlers.api.adapter.context.ExecutionContextAdapter;
import io.gravitee.gateway.reactive.handlers.api.adapter.context.RequestAdapter;
import io.reactivex.CompletableEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link ReadWriteStreamAdapter} is a {@link SimpleReadWriteStream} implementation that can be used when calling {@link io.gravitee.gateway.api.Invoker#invoke(ExecutionContext, ReadStream, Handler)}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ReadWriteStreamAdapter extends SimpleReadWriteStream<Buffer> {

    private final Logger log = LoggerFactory.getLogger(ReadWriteStreamAdapter.class);

    /**
     * Creates an dedicated {@link io.gravitee.gateway.api.stream.ReadWriteStream}.
     * This constructor take an {@link ExecutionContextAdapter} on which a {@link RequestAdapter} is attached.
     * This specific {@link RequestAdapter} allows to register an resume callback that will be invoked by the {@link io.gravitee.gateway.api.Invoker} when it will be ready to read request body.
     *
     * @param ctx an {@link ExecutionContextAdapter} on which a {@link RequestAdapter} is attached and that can be used to
     * @param nextEmitter the reactive emitter that can be used to emit error in case of error.
     */
    public ReadWriteStreamAdapter(ExecutionContextAdapter ctx, CompletableEmitter nextEmitter) {
        final RequestAdapter request = ctx.request();
        final SyncRequest syncRequest = ((SyncExecutionContext) ctx.getDelegate()).request();

        request.onResume(
            () ->
                syncRequest
                    .content()
                    .doOnNext(this::write)
                    .doOnComplete(this::end)
                    .doOnError(nextEmitter::tryOnError)
                    .onErrorResumeNext(e -> {})
                    .subscribe()
        );
    }
}
