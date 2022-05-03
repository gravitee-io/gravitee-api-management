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
package io.gravitee.elasticsearch.client.http;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PfxOptions;

public class HttpClientPfxSslConfiguration
        implements ClientSslConfiguration {
    private String keystorePath;
    private String keystorePassword;

    public HttpClientPfxSslConfiguration() {
    }

    public HttpClientPfxSslConfiguration(String keystorePath, String keystorePassword) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    @Override
    public KeyCertOptions getVertxWebClientSslKeystoreOptions() {
        PfxOptions pfxOptions = new PfxOptions();
        pfxOptions.setPath(getKeystorePath());
        pfxOptions.setPassword(getKeystorePassword());
        return pfxOptions;
    }
}