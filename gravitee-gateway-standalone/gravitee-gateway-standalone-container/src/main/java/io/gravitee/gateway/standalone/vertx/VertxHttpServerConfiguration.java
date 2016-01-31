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
package io.gravitee.gateway.standalone.vertx;

import io.vertx.core.http.HttpServerOptions;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxHttpServerConfiguration {

    @Value("${http.port:8082}")
    private int port;

    @Value("${http.secured:false}")
    private boolean secured;

    @Value("${http.ssl.clientAuth:false}")
    private boolean clientAuth;

    @Value("${http.ssl.keystore.path:#{null}}")
    private String keyStorePath;

    @Value("${http.ssl.keystore.password:#{null}}")
    private String keyStorePassword;

    @Value("${http.ssl.truststore.path:#{null}}")
    private String trustStorePath;

    @Value("${http.ssl.truststore.password:#{null}}")
    private String trustStorePassword;

    @Value("${http.compressionSupported:" + HttpServerOptions.DEFAULT_COMPRESSION_SUPPORTED + "}")
    private boolean compressionSupported;

    @Value("${http.idleTimeout:" + HttpServerOptions.DEFAULT_IDLE_TIMEOUT + "}")
    private int idleTimeout;

    @Value("${http.tcpKeepAlive:true}")
    private boolean tcpKeepAlive;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public boolean isClientAuth() {
        return clientAuth;
    }

    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    public boolean isCompressionSupported() {
        return compressionSupported;
    }

    public void setCompressionSupported(boolean compressionSupported) {
        this.compressionSupported = compressionSupported;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public boolean isTcpKeepAlive() {
        return tcpKeepAlive;
    }

    public void setTcpKeepAlive(boolean tcpKeepAlive) {
        this.tcpKeepAlive = tcpKeepAlive;
    }
}
