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

import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import java.util.Base64;

public abstract class AbstractBaseClient {

    protected void configureTrustStore(final ClientOptionsBase clientOptions, SslOptions sslOptions, String target) {
        if (!sslOptions.isTrustAll() && sslOptions.getTrustStore() != null) {
            switch (sslOptions.getTrustStore().getType()) {
                case PEM:
                    final PEMTrustStore pemTrustStore = (PEMTrustStore) sslOptions.getTrustStore();
                    final PemTrustOptions pemTrustOptions = new PemTrustOptions();

                    if (pemTrustStore.getPath() != null && !pemTrustStore.getPath().isEmpty()) {
                        pemTrustOptions.addCertPath(pemTrustStore.getPath());
                    } else if (pemTrustStore.getContent() != null && !pemTrustStore.getContent().isEmpty()) {
                        pemTrustOptions.addCertValue(io.vertx.core.buffer.Buffer.buffer(pemTrustStore.getContent()));
                    } else {
                        throw new IllegalArgumentException("Missing PEM certificate value for " + target);
                    }

                    clientOptions.setPemTrustOptions(pemTrustOptions);
                    break;
                case PKCS12:
                    final PKCS12TrustStore pkcs12TrustStore = (PKCS12TrustStore) sslOptions.getTrustStore();
                    final PfxOptions pfxOptions = new PfxOptions();

                    if (pkcs12TrustStore.getPath() != null && !pkcs12TrustStore.getPath().isEmpty()) {
                        pfxOptions.setPath(pkcs12TrustStore.getPath());
                    } else if (pkcs12TrustStore.getContent() != null && !pkcs12TrustStore.getContent().isEmpty()) {
                        pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(pkcs12TrustStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing PKCS12 truststore value for " + target);
                    }

                    pfxOptions.setAlias(pkcs12TrustStore.getAlias());
                    pfxOptions.setPassword(pkcs12TrustStore.getPassword());
                    clientOptions.setPfxTrustOptions(pfxOptions);
                    break;
                case JKS:
                    final JKSTrustStore jksTrustStore = (JKSTrustStore) sslOptions.getTrustStore();
                    final JksOptions jksOptions = new JksOptions();

                    if (jksTrustStore.getPath() != null && !jksTrustStore.getPath().isEmpty()) {
                        jksOptions.setPath(jksTrustStore.getPath());
                    } else if (jksTrustStore.getContent() != null && !jksTrustStore.getContent().isEmpty()) {
                        jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(jksTrustStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing JKS truststore value for " + target);
                    }

                    jksOptions.setAlias(jksTrustStore.getAlias());
                    jksOptions.setPassword(jksTrustStore.getPassword());
                    clientOptions.setTrustStoreOptions(jksOptions);
                    break;
            }
        }
    }

    protected void configureKeyStore(final ClientOptionsBase clientOptions, SslOptions sslOptions, String target) {
        if (sslOptions.getKeyStore() != null) {
            switch (sslOptions.getKeyStore().getType()) {
                case PEM:
                    final PEMKeyStore pemKeyStore = (PEMKeyStore) sslOptions.getKeyStore();
                    final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();

                    if (pemKeyStore.getCertPath() != null && !pemKeyStore.getCertPath().isEmpty()) {
                        pemKeyCertOptions.setCertPath(pemKeyStore.getCertPath());
                    } else if (pemKeyStore.getCertContent() != null && !pemKeyStore.getCertContent().isEmpty()) {
                        pemKeyCertOptions.setCertValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getCertContent()));
                    } else {
                        throw new IllegalArgumentException("Missing PEM certificate value for " + target);
                    }

                    if (pemKeyStore.getKeyPath() != null && !pemKeyStore.getKeyPath().isEmpty()) {
                        pemKeyCertOptions.setKeyPath(pemKeyStore.getKeyPath());
                    } else if (pemKeyStore.getKeyContent() != null && !pemKeyStore.getKeyContent().isEmpty()) {
                        pemKeyCertOptions.setKeyValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getKeyContent()));
                    } else {
                        throw new IllegalArgumentException("Missing PEM key value for " + target);
                    }

                    clientOptions.setPemKeyCertOptions(pemKeyCertOptions);
                    break;
                case PKCS12:
                    final PKCS12KeyStore pkcs12KeyStore = (PKCS12KeyStore) sslOptions.getKeyStore();
                    final PfxOptions pfxOptions = new PfxOptions();

                    if (pkcs12KeyStore.getPath() != null && !pkcs12KeyStore.getPath().isEmpty()) {
                        pfxOptions.setPath(pkcs12KeyStore.getPath());
                    } else if (pkcs12KeyStore.getContent() != null && !pkcs12KeyStore.getContent().isEmpty()) {
                        pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(pkcs12KeyStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing PKCS12 keystore value for " + target);
                    }

                    pfxOptions.setAlias(pkcs12KeyStore.getAlias());
                    pfxOptions.setAliasPassword(pkcs12KeyStore.getKeyPassword());
                    pfxOptions.setPassword(pkcs12KeyStore.getPassword());
                    clientOptions.setPfxKeyCertOptions(pfxOptions);
                    break;
                case JKS:
                    final JKSKeyStore jksKeyStore = (JKSKeyStore) sslOptions.getKeyStore();
                    final JksOptions jksOptions = new JksOptions();

                    if (jksKeyStore.getPath() != null && !jksKeyStore.getPath().isEmpty()) {
                        jksOptions.setPath(jksKeyStore.getPath());
                    } else if (jksKeyStore.getContent() != null && !jksKeyStore.getContent().isEmpty()) {
                        jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(jksKeyStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing JKS keystore value for " + target);
                    }

                    jksOptions.setAlias(jksKeyStore.getAlias());
                    jksOptions.setAliasPassword(jksKeyStore.getKeyPassword());
                    jksOptions.setPassword(jksKeyStore.getPassword());
                    clientOptions.setKeyStoreOptions(jksOptions);
                    break;
            }
        }
    }
}
