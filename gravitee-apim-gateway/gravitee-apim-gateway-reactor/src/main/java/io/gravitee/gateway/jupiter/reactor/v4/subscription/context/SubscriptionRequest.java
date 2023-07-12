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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.*;
import java.util.Collections;
import javax.net.ssl.SSLSession;

/**
 * This is a sort of fake {@link MutableRequest} to deal with subscription.
 * Some methods are returning a {@link IllegalStateException} because the methods doesn't make sense
 * in the case of subscription where the request is initiated by an incoming {@link Subscription}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRequest implements MutableRequest {

    private final HttpHeaders headers = HttpHeaders.create();

    private final long timestamp = System.currentTimeMillis();
    private final String id;
    private String transactionId;

    private static final MultiValueMap<String, String> EMPTY_MAP = new LinkedMultiValueMap<>(Collections.emptyMap());

    public SubscriptionRequest(Subscription subscription) {
        this.id = subscription.getId();
        this.transactionId = subscription.getId();
    }

    @Override
    public MutableRequest contextPath(String contextPath) {
        return this;
    }

    @Override
    public MutableRequest pathInfo(String pathInfo) {
        return this;
    }

    @Override
    public MutableRequest transactionId(String id) {
        this.transactionId = id;
        return this;
    }

    @Override
    public MutableRequest remoteAddress(String remoteAddress) {
        return this;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String transactionId() {
        return this.transactionId;
    }

    @Override
    public String uri() {
        throw new IllegalStateException();
    }

    @Override
    public String host() {
        throw new IllegalStateException();
    }

    @Override
    public String path() {
        throw new IllegalStateException();
    }

    @Override
    public String pathInfo() {
        throw new IllegalStateException();
    }

    @Override
    public String contextPath() {
        throw new IllegalStateException();
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        return EMPTY_MAP;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        return EMPTY_MAP;
    }

    @Override
    public HttpHeaders headers() {
        return this.headers;
    }

    @Override
    public HttpMethod method() {
        throw new IllegalStateException();
    }

    @Override
    public String scheme() {
        throw new IllegalStateException();
    }

    @Override
    public HttpVersion version() {
        throw new IllegalStateException();
    }

    @Override
    public long timestamp() {
        return this.timestamp;
    }

    @Override
    public String remoteAddress() {
        throw new IllegalStateException();
    }

    @Override
    public String localAddress() {
        throw new IllegalStateException();
    }

    @Override
    public SSLSession sslSession() {
        throw new IllegalStateException();
    }

    @Override
    public Metrics metrics() {
        return null;
    }

    @Override
    public boolean ended() {
        return false;
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
        return Maybe.empty();
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

    @Override
    public Flowable<Message> messages() {
        return Flowable.empty();
    }

    @Override
    public void messages(Flowable<Message> messages) {}

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        return Completable.complete();
    }
}
