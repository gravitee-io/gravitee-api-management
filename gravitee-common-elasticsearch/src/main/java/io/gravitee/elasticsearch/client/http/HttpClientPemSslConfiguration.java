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
import io.vertx.core.net.PemKeyCertOptions;

import java.util.List;

public class HttpClientPemSslConfiguration
        implements ClientSslConfiguration {
    private List<String> certs;
    private List<String> keys;

    public HttpClientPemSslConfiguration() {
    }

    public HttpClientPemSslConfiguration(List<String> certs, List<String> keys) {
        this.certs = certs;
        this.keys = keys;
    }

    public List<String> getCerts() {
        return certs;
    }

    public void setCerts(List<String> certs) {
        this.certs = certs;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    @Override
    public KeyCertOptions getVertxWebClientSslKeystoreOptions() {
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        pemKeyCertOptions.setCertPaths(certs);
        pemKeyCertOptions.setKeyPaths(keys);
        return pemKeyCertOptions;
    }
}
