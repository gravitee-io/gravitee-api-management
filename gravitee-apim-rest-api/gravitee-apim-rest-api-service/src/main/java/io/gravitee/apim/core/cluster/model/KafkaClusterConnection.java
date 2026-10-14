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
package io.gravitee.apim.core.cluster.model;

import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.definition.model.cluster.SecurityProtocol;
import io.gravitee.plugin.configurations.ssl.KeyStore;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.TrustStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
import java.util.Set;

public record KafkaClusterConnection(String crossId, String name, String bootstrapServers, SecurityConfig security) {
    /** SASL mechanisms the Kafka endpoint of Message APIs supports (ESM-261 adds the others). */
    private static final Set<String> MESSAGE_API_SASL_MECHANISMS = Set.of("PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512");

    /**
     * Why a Message API cannot use this connection, or {@code null} when it can: the single rule behind the validation on
     * API save and the connection pickers. The Kafka endpoint refuses the same connections at runtime. Not a bean getter:
     * the connection is serialized as a record, and this must not become a property.
     */
    public String messageApiUnsupportedReason() {
        SecurityProtocol protocol = security == null || security.protocol() == null ? SecurityProtocol.PLAINTEXT : security.protocol();
        if (protocol == SecurityProtocol.SASL_PLAINTEXT || protocol == SecurityProtocol.SASL_SSL) {
            String mechanism = security.sasl() == null || security.sasl().mechanism() == null ? null : security.sasl().mechanism().type();
            if (mechanism == null || "NONE".equals(mechanism)) {
                return "It uses the " + protocol + " protocol without a SASL mechanism.";
            }
            if (!MESSAGE_API_SASL_MECHANISMS.contains(mechanism)) {
                return "Message APIs do not support the SASL mechanism " + mechanism + " yet.";
            }
        }
        if ((protocol == SecurityProtocol.SSL || protocol == SecurityProtocol.SASL_SSL) && security.ssl() != null) {
            return sslUnsupportedReason(security.ssl());
        }
        return null;
    }

    private static String sslUnsupportedReason(SslOptions ssl) {
        if (ssl.isTrustAll()) {
            return "Trust all is not supported: give a truststore.";
        }
        TrustStore trustStore = ssl.getTrustStore();
        if (trustStore instanceof PEMTrustStore pem && !hasText(pem.getContent()) && !hasText(pem.getPath())) {
            return "Its PEM truststore has neither content nor path.";
        }
        if (
            (trustStore instanceof JKSTrustStore jks && !hasText(jks.getPath())) ||
            (trustStore instanceof PKCS12TrustStore pkcs12 && !hasText(pkcs12.getPath()))
        ) {
            return "Its truststore must be given by path, not by content.";
        }
        KeyStore keyStore = ssl.getKeyStore();
        if (keyStore instanceof PEMKeyStore pem && (!hasText(pem.getKeyContent()) || !hasText(pem.getCertContent()))) {
            return "Its PEM keystore must be given by key and certificate content, not by path.";
        }
        if (
            (keyStore instanceof JKSKeyStore jks && !hasText(jks.getPath())) ||
            (keyStore instanceof PKCS12KeyStore pkcs12 && !hasText(pkcs12.getPath()))
        ) {
            return "Its keystore must be given by path, not by content.";
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
