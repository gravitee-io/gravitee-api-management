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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.ssl.KeyStore;
import io.gravitee.definition.model.ssl.TrustStore;
import java.io.Serializable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientSslOptions implements Serializable {

    @JsonProperty("trustAll")
    private boolean trustAll;

    @JsonProperty("hostnameVerifier")
    private boolean hostnameVerifier;

    @JsonProperty("trustStore")
    private TrustStore trustStore;

    @JsonProperty("keyStore")
    private KeyStore keyStore;

    public boolean isHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(boolean hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public boolean isTrustAll() {
        return trustAll;
    }

    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }
}
