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
package io.gravitee.repository.bridge.server.http.configuration;

import io.vertx.core.http.HttpServerOptions;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpServerConfiguration {

    @Value("${services.bridge.http.enabled:true}")
    private boolean enabled;

    @Value("${services.bridge.http.port:18092}")
    private int port;

    @Value("${services.bridge.http.host:localhost}")
    private String host;

    @Value("${services.bridge.http.authentication.type:basic}")
    private String authenticationType;

    @Value("${services.bridge.http.secured:false}")
    private boolean secured;

    @Value("${services.bridge.http.alpn:false}")
    private boolean alpn;

    @Value("${services.bridge.http.ssl.clientAuth:false}")
    private boolean clientAuth;

    @Value("${services.bridge.http.ssl.keystore.type:#{null}}")
    private String keyStoreType;

    @Value("${services.bridge.http.ssl.keystore.path:#{null}}")
    private String keyStorePath;

    @Value("${services.bridge.http.ssl.keystore.password:#{null}}")
    private String keyStorePassword;

    @Value("${services.bridge.http.ssl.truststore.type:#{null}}")
    private String trustStoreType;

    @Value("${services.bridge.http.ssl.truststore.path:#{null}}")
    private String trustStorePath;

    @Value("${services.bridge.http.ssl.truststore.password:#{null}}")
    private String trustStorePassword;

    @Value("${services.bridge.http.idleTimeout:" + HttpServerOptions.DEFAULT_IDLE_TIMEOUT + "}")
    private int idleTimeout;

    @Value("${services.bridge.http.tcpKeepAlive:true}")
    private boolean tcpKeepAlive;

    @Value("${services.bridge.http.maxHeaderSize:8192}")
    private int maxHeaderSize;

    @Value("${services.bridge.http.maxChunkSize:8192}")
    private int maxChunkSize;
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAuthenticationType() {
      return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
      this.authenticationType = authenticationType;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public boolean isAlpn() {
        return alpn;
    }

    public void setAlpn(boolean alpn) {
        this.alpn = alpn;
    }

    public boolean isClientAuth() {
        return clientAuth;
    }

    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
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

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
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

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }
}
