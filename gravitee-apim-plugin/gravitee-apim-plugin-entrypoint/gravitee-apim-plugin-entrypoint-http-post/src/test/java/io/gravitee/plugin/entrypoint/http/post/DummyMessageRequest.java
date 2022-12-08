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
package io.gravitee.plugin.entrypoint.http.post;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Single;
import javax.net.ssl.SSLSession;

public class DummyMessageRequest implements Request {

    Flowable<Message> flowableMessages;
    HttpMethod method;
    Maybe<Buffer> body;
    HttpHeaders headers;
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

    public void setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    @Override
    public Flowable<Message> messages() {
        return flowableMessages;
    }

    @Override
    public void messages(Flowable<Message> messages) {
        flowableMessages = messages;
    }

    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    public void method(HttpMethod method) {
        this.method = method;
    }

    @Override
    public boolean isWebSocket() {
        return false;
    }

    @Override
    public WebSocket webSocket() {
        return null;
    }

    @Override
    public Maybe<Buffer> body() {
        return body;
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
    public Flowable<Buffer> chunks() {
        return null;
    }

    @Override
    public void chunks(Flowable<Buffer> chunks) {}

    @Override
    public Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks) {
        return null;
    }

    public void body(Maybe<Buffer> body) {
        this.body = body;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    public void headers(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public String id() {
        return null;
    }

    @Override
    public String transactionId() {
        return null;
    }

    @Override
    public String clientIdentifier() {
        return null;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public String host() {
        return null;
    }

    @Override
    public String originalHost() {
        return null;
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public String pathInfo() {
        return null;
    }

    @Override
    public String contextPath() {
        return null;
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        return null;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        return null;
    }

    @Override
    public String scheme() {
        return null;
    }

    @Override
    public HttpVersion version() {
        return httpVersion;
    }

    @Override
    public long timestamp() {
        return 0;
    }

    @Override
    public String remoteAddress() {
        return null;
    }

    @Override
    public String localAddress() {
        return null;
    }

    @Override
    public SSLSession sslSession() {
        return null;
    }

    @Override
    public boolean ended() {
        return false;
    }

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        return Completable.complete();
    }

    @Override
    public void contentLength(long l) {}
}
