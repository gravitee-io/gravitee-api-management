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
package io.gravitee.gateway.jupiter.reactor.v4.subscription.context;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.context.HttpResponse;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.reactivex.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResponse implements MutableResponse {

    private HttpHeaders headers = HttpHeaders.create();

    private int statusCode = HttpStatusCode.OK_200;

    private String reason;

    private Flowable<Message> messages;

    @Override
    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public HttpResponse status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @Override
    public int status() {
        return this.statusCode;
    }

    @Override
    public String reason() {
        return this.reason;
    }

    @Override
    public HttpResponse reason(String message) {
        this.reason = message;
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public HttpHeaders trailers() {
        return null;
    }

    @Override
    public boolean ended() {
        return false;
    }

    @Override
    public Flowable<Message> messages() {
        return this.messages;
    }

    @Override
    public void messages(Flowable<Message> messages) {
        this.messages = messages;
    }

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        return Completable.complete();
    }

    @Override
    public Maybe<Buffer> body() {
        return null;
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return null;
    }

    @Override
    public void body(Buffer buffer) {}

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        return null;
    }

    @Override
    public void chunks(Flowable<Buffer> chunks) {}

    @Override
    public Flowable<Buffer> chunks() {
        return null;
    }

    @Override
    public Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks) {
        return null;
    }

    @Override
    public Completable end() {
        return Completable.complete();
    }

    @Override
    public Completable end(Buffer buffer) {
        return Completable.complete();
    }

    @Override
    public Completable write(Buffer buffer) {
        return Completable.complete();
    }

    @Override
    public Completable writeHeaders() {
        return Completable.complete();
    }
}
