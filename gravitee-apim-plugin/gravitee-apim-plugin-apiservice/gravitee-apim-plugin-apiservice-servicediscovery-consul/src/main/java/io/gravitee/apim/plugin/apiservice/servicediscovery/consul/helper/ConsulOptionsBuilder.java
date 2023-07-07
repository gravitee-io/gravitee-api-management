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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul.helper;

import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryService.CONSUL_SERVICE_DISCOVERY_ID;

import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryServiceConfiguration;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.consul.ConsulClientOptions;
import java.net.URI;
import java.util.Base64;

public class ConsulOptionsBuilder {

    private static final String HTTPS_SCHEME = "https";
    private static final int CONSUL_DEFAULT_PORT = 8500;

    private ConsulOptionsBuilder() {}

    public static ConsulClientOptions from(ConsulServiceDiscoveryServiceConfiguration configuration) {
        URI consulUri = URI.create(configuration.getUrl());
        var options = new ConsulClientOptions()
            .setHost(consulUri.getHost())
            .setPort((consulUri.getPort() != -1) ? consulUri.getPort() : CONSUL_DEFAULT_PORT)
            .setDc(configuration.getDc())
            .setAclToken(configuration.getAcl());

        if (HTTPS_SCHEME.equalsIgnoreCase(consulUri.getScheme())) {
            // SSL is not configured but the endpoint scheme is HTTPS so let's enable the SSL on Vert.x HTTP client
            options.setSsl(true);
        }

        if (configuration.getSslOptions() != null) {
            options
                .setVerifyHost(configuration.getSslOptions().isHostnameVerifier())
                .setTrustAll(configuration.getSslOptions().isTrustAll());
            configureKeyStore(configuration.getSslOptions(), options);
            configureTrustStore(configuration.getSslOptions(), options);
        }
        return options;
    }

    private static void configureKeyStore(SslOptions sslOptions, ConsulClientOptions options) {
        if (sslOptions.getKeyStore() != null) {
            switch (sslOptions.getKeyStore().getType()) {
                case PEM:
                    options.setPemKeyCertOptions(buildPemKeyCertOptions(sslOptions));
                    break;
                case PKCS12:
                    options.setPfxKeyCertOptions(buildPfxKeyCertOptions(sslOptions));
                    break;
                case JKS:
                    options.setKeyStoreOptions(buildJksKeyCertOptions(sslOptions));
            }
        }
    }

    private static void configureTrustStore(SslOptions sslOptions, ConsulClientOptions options) {
        if (!sslOptions.isTrustAll() && sslOptions.getTrustStore() != null) {
            switch (sslOptions.getTrustStore().getType()) {
                case PEM:
                    options.setPemTrustOptions(buildPemTrustOptions(sslOptions));
                    break;
                case PKCS12:
                    options.setPfxTrustOptions(buildPfxTrustOptions(sslOptions));
                    break;
                case JKS:
                    JksOptions jksOptions = buildJksTrustOptions(sslOptions);
                    options.setTrustStoreOptions(jksOptions);
            }
        }
    }

    private static PemKeyCertOptions buildPemKeyCertOptions(SslOptions sslOptions) {
        PEMKeyStore pemKeyStore = (PEMKeyStore) sslOptions.getKeyStore();
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();

        if (pemKeyStore.getCertPath() != null && !pemKeyStore.getCertPath().isEmpty()) {
            pemKeyCertOptions.setCertPath(pemKeyStore.getCertPath());
        } else {
            if (pemKeyStore.getCertContent() == null || pemKeyStore.getCertContent().isEmpty()) {
                throw new IllegalArgumentException("Missing PEM certificate value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            pemKeyCertOptions.setCertValue(Buffer.buffer(pemKeyStore.getCertContent()));
        }

        if (pemKeyStore.getKeyPath() != null && !pemKeyStore.getKeyPath().isEmpty()) {
            pemKeyCertOptions.setKeyPath(pemKeyStore.getKeyPath());
        } else {
            if (pemKeyStore.getKeyContent() == null || pemKeyStore.getKeyContent().isEmpty()) {
                throw new IllegalArgumentException("Missing PEM key value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            pemKeyCertOptions.setKeyValue(Buffer.buffer(pemKeyStore.getKeyContent()));
        }

        return pemKeyCertOptions;
    }

    private static PfxOptions buildPfxKeyCertOptions(SslOptions sslOptions) {
        PKCS12KeyStore pkcs12KeyStore = (PKCS12KeyStore) sslOptions.getKeyStore();
        PfxOptions pfxOptions = new PfxOptions();
        if (pkcs12KeyStore.getPath() != null && !pkcs12KeyStore.getPath().isEmpty()) {
            pfxOptions.setPath(pkcs12KeyStore.getPath());
        } else {
            if (pkcs12KeyStore.getContent() == null || pkcs12KeyStore.getContent().isEmpty()) {
                throw new IllegalArgumentException("Missing PKCS12 keystore value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            pfxOptions.setValue(Buffer.buffer(Base64.getDecoder().decode(pkcs12KeyStore.getContent())));
        }

        pfxOptions.setAlias(pkcs12KeyStore.getAlias());
        pfxOptions.setAliasPassword(pkcs12KeyStore.getKeyPassword());
        pfxOptions.setPassword(pkcs12KeyStore.getPassword());

        return pfxOptions;
    }

    private static JksOptions buildJksKeyCertOptions(SslOptions sslOptions) {
        JKSKeyStore jksKeyStore = (JKSKeyStore) sslOptions.getKeyStore();
        JksOptions jksOptions = new JksOptions();
        if (jksKeyStore.getPath() != null && !jksKeyStore.getPath().isEmpty()) {
            jksOptions.setPath(jksKeyStore.getPath());
        } else {
            if (jksKeyStore.getContent() == null || jksKeyStore.getContent().isEmpty()) {
                throw new IllegalArgumentException("Missing JKS keystore value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            jksOptions.setValue(Buffer.buffer(Base64.getDecoder().decode(jksKeyStore.getContent())));
        }

        jksOptions.setAlias(jksKeyStore.getAlias());
        jksOptions.setAliasPassword(jksKeyStore.getKeyPassword());
        jksOptions.setPassword(jksKeyStore.getPassword());

        return jksOptions;
    }

    private static PemTrustOptions buildPemTrustOptions(SslOptions sslOptions) {
        PEMTrustStore pemTrustStore = (PEMTrustStore) sslOptions.getTrustStore();
        PemTrustOptions pemTrustOptions = new PemTrustOptions();
        if (pemTrustStore.getPath() != null && !pemTrustStore.getPath().isEmpty()) {
            pemTrustOptions.addCertPath(pemTrustStore.getPath());
        } else {
            if (pemTrustStore.getContent() == null || pemTrustStore.getContent().isEmpty()) {
                throw new IllegalArgumentException("Missing PEM certificate value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            pemTrustOptions.addCertValue(Buffer.buffer(pemTrustStore.getContent()));
        }
        return pemTrustOptions;
    }

    private static PfxOptions buildPfxTrustOptions(SslOptions sslOptions) {
        PKCS12TrustStore pkcs12TrustStore = (PKCS12TrustStore) sslOptions.getTrustStore();
        PfxOptions pfxOptions = new PfxOptions();
        if (pkcs12TrustStore.getPath() != null && !pkcs12TrustStore.getPath().isEmpty()) {
            pfxOptions.setPath(pkcs12TrustStore.getPath());
        } else {
            if (pkcs12TrustStore.getContent() == null || pkcs12TrustStore.getContent().isEmpty()) {
                throw new IllegalArgumentException("Missing PKCS12 truststore value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            pfxOptions.setValue(Buffer.buffer(Base64.getDecoder().decode(pkcs12TrustStore.getContent())));
        }

        pfxOptions.setAlias(pkcs12TrustStore.getAlias());
        pfxOptions.setPassword(pkcs12TrustStore.getPassword());
        return pfxOptions;
    }

    private static JksOptions buildJksTrustOptions(SslOptions sslOptions) {
        JKSTrustStore jksTrustStore = (JKSTrustStore) sslOptions.getTrustStore();
        JksOptions jksOptions = new JksOptions();
        if (jksTrustStore.getPath() != null && !jksTrustStore.getPath().isEmpty()) {
            jksOptions.setPath(jksTrustStore.getPath());
        } else {
            if (jksTrustStore.getContent() == null || jksTrustStore.getContent().isEmpty()) {
                throw new IllegalArgumentException("Missing JKS truststore value for " + CONSUL_SERVICE_DISCOVERY_ID);
            }

            jksOptions.setValue(Buffer.buffer(Base64.getDecoder().decode(jksTrustStore.getContent())));
        }

        jksOptions.setAlias(jksTrustStore.getAlias());
        jksOptions.setPassword(jksTrustStore.getPassword());
        return jksOptions;
    }
}
