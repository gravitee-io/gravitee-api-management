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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientOptions implements Serializable {

    public static long DEFAULT_IDLE_TIMEOUT = 60000;
    public static long DEFAULT_KEEP_ALIVE_TIMEOUT = 30000;
    public static long DEFAULT_CONNECT_TIMEOUT = 5000;
    public static long DEFAULT_READ_TIMEOUT = 10000;
    public static int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 100;
    public static boolean DEFAULT_KEEP_ALIVE = true;
    public static boolean DEFAULT_PIPELINING = false;
    public static boolean DEFAULT_USE_COMPRESSION = true;
    public static boolean DEFAULT_PROPAGATE_CLIENT_ACCEPT_ENCODING = false;
    public static boolean DEFAULT_FOLLOW_REDIRECTS = false;
    public static boolean DEFAULT_CLEAR_TEXT_UPGRADE = true;
    public static ProtocolVersion DEFAULT_PROTOCOL_VERSION = ProtocolVersion.HTTP_1_1;

    @JsonProperty("idleTimeout")
    private long idleTimeout = DEFAULT_IDLE_TIMEOUT;

    @JsonProperty("keepAliveTimeout")
    private long keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

    @JsonProperty("connectTimeout")
    private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @JsonProperty("keepAlive")
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;

    @JsonProperty("readTimeout")
    private long readTimeout = DEFAULT_READ_TIMEOUT;

    @JsonProperty("pipelining")
    private boolean pipelining = DEFAULT_PIPELINING;

    @JsonProperty("maxConcurrentConnections")
    private int maxConcurrentConnections = DEFAULT_MAX_CONCURRENT_CONNECTIONS;

    @JsonProperty("useCompression")
    private boolean useCompression = DEFAULT_USE_COMPRESSION;

    @JsonProperty("propagateClientAcceptEncoding")
    private boolean propagateClientAcceptEncoding = DEFAULT_PROPAGATE_CLIENT_ACCEPT_ENCODING;

    @JsonProperty("followRedirects")
    private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;

    @JsonProperty("clearTextUpgrade")
    private Boolean clearTextUpgrade = DEFAULT_CLEAR_TEXT_UPGRADE;

    @JsonProperty("version")
    private ProtocolVersion version = DEFAULT_PROTOCOL_VERSION;

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isPipelining() {
        return pipelining;
    }

    public void setPipelining(boolean pipelining) {
        this.pipelining = pipelining;
    }

    public int getMaxConcurrentConnections() {
        return maxConcurrentConnections;
    }

    public void setMaxConcurrentConnections(int maxConcurrentConnections) {
        this.maxConcurrentConnections = maxConcurrentConnections;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    public boolean isPropagateClientAcceptEncoding() {
        return propagateClientAcceptEncoding;
    }

    public void setPropagateClientAcceptEncoding(boolean propagateClientAcceptEncoding) {
        this.propagateClientAcceptEncoding = propagateClientAcceptEncoding;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public boolean isClearTextUpgrade() {
        return clearTextUpgrade;
    }

    public void setClearTextUpgrade(boolean clearTextUpgrade) {
        this.clearTextUpgrade = clearTextUpgrade;
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }
}
