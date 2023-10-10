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
package io.gravitee.gateway.tcp.reactive.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @author Thomas Segismont
 */
public class RecordingWriteStream implements WriteStream<Buffer> {

    private final List<Buffer> recordedBuffers;
    private AtomicReference<Throwable> err = new AtomicReference<>();

    public RecordingWriteStream() {
        this.recordedBuffers = Collections.synchronizedList(new ArrayList<>());
    }

    public void errOnNextWrite(Throwable err) {
        this.err.set(err);
    }

    @Override
    public RecordingWriteStream exceptionHandler(Handler<Throwable> exceptionHandler) {
        return this;
    }

    @Override
    public Future<Void> write(Buffer data) {
        if (err.get() != null) {
            return Future.<Void>failedFuture(this.err.get()).onComplete(empty -> this.err = null);
        }
        this.recordedBuffers.add(data);
        return Future.succeededFuture();
    }

    @Override
    public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
        handler.handle(write(data));
    }

    @Override
    public Future<Void> end() {
        return Future.succeededFuture();
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        handler.handle(end());
    }

    @Override
    public RecordingWriteStream setWriteQueueMaxSize(int maxSize) {
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public RecordingWriteStream drainHandler(Handler<Void> drainHandler) {
        return this;
    }

    public List<String> getRecordedBuffers() {
        return recordedBuffers.stream().map(Object::toString).toList();
    }
}
