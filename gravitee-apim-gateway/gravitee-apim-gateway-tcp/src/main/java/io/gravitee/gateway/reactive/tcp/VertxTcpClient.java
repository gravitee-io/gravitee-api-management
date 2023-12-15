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

import io.gravitee.common.util.VertxProxyOptionsUtils;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.tcp.TcpClientOptions;
import io.gravitee.definition.model.v4.tcp.TcpProxyOptions;
import io.gravitee.definition.model.v4.tcp.TcpTarget;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetClient;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class VertxTcpClient extends AbstractBaseClient {

    protected static final String TCP_SSL_OPENSSL_CONFIGURATION = "tcp.ssl.openssl";

    @NonNull
    private final Vertx vertx;

    @NonNull
    private final Configuration nodeConfiguration;

    private final TcpTarget tcpTarget;
    private TcpClientOptions tcpOptions;
    private SslOptions sslOptions;
    private TcpProxyOptions proxyOptions;

    public NetClient createTcpClient() {
        if (tcpOptions == null) {
            tcpOptions = new TcpClientOptions();
        }

        return vertx.createNetClient(createNetClientOptions());
    }

    private NetClientOptions createNetClientOptions() {
        var clientOptions = new NetClientOptions().setMetricsName("tcp-client");
        clientOptions
            .setConnectTimeout(tcpOptions.getConnectTimeout())
            .setReconnectAttempts(tcpOptions.getReconnectAttempts())
            .setReconnectInterval(tcpOptions.getReconnectInterval())
            .setIdleTimeout(tcpOptions.getIdleTimeout())
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
            .setReadIdleTimeout(tcpOptions.getIdleTimeout())
            .setWriteIdleTimeout(tcpOptions.getIdleTimeout());

        configureSsl(clientOptions);
        configureTcpProxy(clientOptions);

        return clientOptions;
    }

    private void configureTcpProxy(NetClientOptions clientOptions) {
        if (proxyOptions != null && proxyOptions.isEnabled()) {
            if (proxyOptions.isUseSystemProxy()) {
                setSystemProxy(clientOptions);
            } else {
                ProxyOptions newProxyOptions = new ProxyOptions();
                newProxyOptions.setHost(proxyOptions.getHost());
                newProxyOptions.setPort(proxyOptions.getPort());
                newProxyOptions.setUsername(proxyOptions.getUsername());
                newProxyOptions.setPassword(proxyOptions.getPassword());
                newProxyOptions.setType(ProxyType.valueOf(this.proxyOptions.getType().name()));
                clientOptions.setProxyOptions(newProxyOptions);
            }
        }
    }

    private void setSystemProxy(NetClientOptions clientOptions) {
        try {
            clientOptions.setProxyOptions(VertxProxyOptionsUtils.buildProxyOptions(nodeConfiguration));
        } catch (Exception e) {
            log.warn(
                "TcpClient (target[{}]) requires a system proxy to be defined but some configurations are missing or not well defined: {}",
                tcpTarget,
                e.getMessage()
            );
            log.warn("Ignoring system proxy");
        }
    }

    private void configureSsl(final NetClientOptions clientOptions) {
        clientOptions.setSsl(tcpTarget.isSecured());
        if (sslOptions != null) {
            if (nodeConfiguration.getProperty(TCP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false)) {
                clientOptions.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            clientOptions.setTrustAll(sslOptions.isTrustAll());

            if (!sslOptions.isTrustAll()) {
                // Client truststore configuration (trust server certificate).
                super.configureTrustStore(clientOptions, sslOptions, tcpTarget.toString());
            }

            // Client keystore configuration (client certificate for mtls).
            super.configureKeyStore(clientOptions, sslOptions, tcpTarget.toString());
        }
    }
}
