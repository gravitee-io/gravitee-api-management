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
package io.gravitee.gateway.reactive.tcp;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.core.DefaultTlsSession;
import io.gravitee.gateway.reactive.core.MessageFlow;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.net.NetSocket;
import io.vertx.rxjava3.core.net.SocketAddress;
import io.vertx.rxjava3.core.streams.WriteStream;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * This class represent the inbound TCP connection.
 * It only holds TCP layer 4 information (SNI, remote and local addresses) as well as metadata (ids, dates) the rest is null.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxTcpRequest extends AbstractRequest {

    private final NetSocket proxySocket;
    private Completable upstreamPipe;

    /**
     * Build a request by assigning fresh {@link #id()} and {@link #transactionId()} using idGenerator.
     * {@link #host()} is based on proxySocket SNI. As well {@link #localAddress()} {@link #remoteAddress()}
     * from corresponding attributes in {@link NetSocket}. {@link #originalHost()}
     * on the other hand is mapped from remoteAddress and might be null and might not match SNI.
     * @param proxySocket Vert.x {@link NetSocket} obtained from {@link io.vertx.rxjava3.core.net.NetServer}
     * @param idGenerator an {@link IdGenerator} instance
     */
    public VertxTcpRequest(NetSocket proxySocket, IdGenerator idGenerator) {
        super();
        this.proxySocket = proxySocket;
        proxySocket.endHandler(empty -> this.ended = true);
        this.id = idGenerator.randomString();
        this.transactionId = idGenerator.randomString();
        this.timestamp = Instant.now().toEpochMilli();
        this.host = proxySocket.indicatedServerName();
        this.chunks(proxySocket.toFlowable().map(Buffer::buffer));
        this.messageFlow = new MessageFlow<>();
        this.contextPath = "";
        this.path = "";
        this.pathInfo = "";
        this.scheme = "";
        this.uri = "";
        this.headers = HttpHeaders.create();
        this.sslSession = proxySocket.sslSession();
        this.tlsSession = new DefaultTlsSession(this.sslSession);
        this.parameters = new LinkedMultiValueMap<>();
        this.pathParameters = new LinkedMultiValueMap<>();
    }

    @Override
    public boolean isStreaming() {
        return true;
    }

    WriteStream<io.vertx.rxjava3.core.buffer.Buffer> getWriteStream() {
        return proxySocket;
    }

    @Override
    public void pipeUpstream(Completable pipe) {
        this.upstreamPipe = pipe;
    }

    Completable upstreamPipe() {
        return upstreamPipe;
    }

    @Override
    public String localAddress() {
        if (this.localAddress == null) {
            this.localAddress = extractAddress(proxySocket.localAddress());
        }
        return this.localAddress;
    }

    private String extractAddress(SocketAddress address) {
        if (address != null) {
            // TO DO Could be improve to a better compatibility with geoIP
            int ipv6Idx = address.host().indexOf("%");
            return (ipv6Idx != -1) ? address.host().substring(0, ipv6Idx) : address.host();
        }
        return null;
    }

    @Override
    public String remoteAddress() {
        if (this.remoteAddress == null) {
            this.remoteAddress = Optional.ofNullable(proxySocket.remoteAddress()).map(Objects::toString).orElse(null);
        }
        return this.remoteAddress;
    }

    @Override
    public String originalHost() {
        if (this.originalHost == null) {
            this.originalHost =
                Optional.ofNullable(proxySocket.remoteAddress()).map(address -> proxySocket.remoteAddress().host()).orElse(null);
        }
        return this.originalHost;
    }
}
