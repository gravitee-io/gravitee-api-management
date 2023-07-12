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
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.function.Function;
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

    protected static final String DEFAULT_LOCALHOST = "localhost";

    private final long timestamp;
    private final String id;
    private final HttpHeaders headers;
    private final Metrics metrics;
    private final LinkedMultiValueMap<String, String> parameters;
    private final LinkedMultiValueMap<String, String> pathParameters;
    private String transactionId;
    private String clientIdentifier;

    public SubscriptionRequest(Subscription subscription) {
        this.id = subscription.getId();
        this.transactionId = subscription.getId();
        this.clientIdentifier = subscription.getId();
        this.timestamp = System.currentTimeMillis();
        this.headers = HttpHeaders.create();
        this.metrics = Metrics.on(timestamp).build();
        this.parameters = new LinkedMultiValueMap<>(Collections.emptyMap());
        this.pathParameters = new LinkedMultiValueMap<>(Collections.emptyMap());
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
    public MutableRequest clientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
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
    public String clientIdentifier() {
        return this.clientIdentifier;
    }

    @Override
    public String uri() {
        return "";
    }

    @Override
    public String host() {
        return DEFAULT_LOCALHOST;
    }

    @Override
    public String originalHost() {
        return DEFAULT_LOCALHOST;
    }

    @Override
    public String path() {
        return "";
    }

    @Override
    public String pathInfo() {
        return "";
    }

    @Override
    public String contextPath() {
        return "";
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        // We still maintain request parameters to be compliant with the execution flow (eg: policies), that could access metrics.
        return parameters;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        // We still maintain request path parameters to be compliant with the execution flow (eg: policies), that could access metrics.
        return pathParameters;
    }

    @Override
    public HttpHeaders headers() {
        // We still maintain request headers to be compliant with the execution flow (eg: policies), that could access metrics.
        return this.headers;
    }

    @Override
    public HttpMethod method() {
        // We still maintain metrics to be compliant with the execution flow (eg: policies), that could access metrics.

        return HttpMethod.OTHER;
    }

    @Override
    public String scheme() {
        // There is no concept of scheme for a subscription request as it is issued internally by the gateway.
        return "";
    }

    @Override
    public HttpVersion version() {
        // There is no real concept of http version for a subscription request as it is issued internally by the gateway.
        return null;
    }

    @Override
    public long timestamp() {
        return this.timestamp;
    }

    @Override
    public String remoteAddress() {
        // Subscription request is issued by the gateway itslef.
        return DEFAULT_LOCALHOST;
    }

    @Override
    public String localAddress() {
        // Subscription request is issued by the gateway internally.
        return DEFAULT_LOCALHOST;
    }

    @Override
    public SSLSession sslSession() {
        // There is no ssl session on a subscription request.
        return null;
    }

    @Override
    public Metrics metrics() {
        // We still maintain metrics to be compliant with the execution flow (eg: policies), that could access metrics.
        return metrics;
    }

    @Override
    public boolean ended() {
        // Subscription request must always be considered has ended because it is issued by the gateway itself and there is no body to consume in that case.
        return true;
    }

    @Override
    public boolean isWebSocket() {
        // A subscription request can't be upgraded to websocket.
        return false;
    }

    @Override
    public WebSocket webSocket() {
        return null;
    }

    @Override
    public Maybe<Buffer> body() {
        // Subscription request has no body because it is issue by the gateway itself.
        return Maybe.empty();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        // Subscription request has no body. Just return an empty buffer.
        return Single.just(Buffer.buffer());
    }

    @Override
    public void body(Buffer buffer) {
        // It is not possible to assign a body to a subscription request.
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        // No transformation on body can be performed on a subscription request.
        return Completable.complete();
    }

    @Override
    public Flowable<Buffer> chunks() {
        // There is no chunks on a subscription request.
        return Flowable.empty();
    }

    @Override
    public void chunks(Flowable<Buffer> chunks) {
        // It is not possible to assign chunks to a subscription request.
    }

    @Override
    public Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks) {
        // No transformation on chunks can be performed on a subscription request.
        return Completable.complete();
    }

    @Override
    public Flowable<Message> messages() {
        // A subscription request is not able to deal with messages.
        return Flowable.empty();
    }

    @Override
    public void messages(Flowable<Message> messages) {
        // It is not possible to assign messages to a subscription request.
    }

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        // No transformation on messages can be performed on a subscription request.
        return Completable.complete();
    }

    @Override
    public void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor) {
        // No message so no interceptor.
    }

    @Override
    public void unsetMessagesInterceptor() {
        // No message so no interceptor.
    }
}
