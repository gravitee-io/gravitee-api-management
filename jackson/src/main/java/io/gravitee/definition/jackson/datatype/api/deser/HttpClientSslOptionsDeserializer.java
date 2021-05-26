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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.ssl.KeyStore;
import io.gravitee.definition.model.ssl.KeyStoreType;
import io.gravitee.definition.model.ssl.TrustStore;
import io.gravitee.definition.model.ssl.TrustStoreType;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientSslOptionsDeserializer extends AbstractStdScalarDeserializer<HttpClientSslOptions> {

    public HttpClientSslOptionsDeserializer(Class<HttpClientSslOptions> vc) {
        super(vc);
    }

    @Override
    public HttpClientSslOptions deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpClientSslOptions httpClientSslOptions = new HttpClientSslOptions();

        JsonNode trustAllNode = node.get("trustAll");
        if (trustAllNode != null) {
            boolean trustAll = trustAllNode.asBoolean(false);
            httpClientSslOptions.setTrustAll(trustAll);
        }

        JsonNode hostnameVerifierNode = node.get("hostnameVerifier");
        if (hostnameVerifierNode != null) {
            boolean hostnameVerifier = hostnameVerifierNode.asBoolean(false);
            httpClientSslOptions.setHostnameVerifier(hostnameVerifier);
        }

        // Ensure backward compatibility with Gravitee.io < 1.20
        String sPem = readStringValue(node, "pem");
        if (sPem != null && !sPem.equals("null")) {
            PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setContent(sPem);
            httpClientSslOptions.setTrustStore(trustStore);
        }

        JsonNode trustStoreNode = node.get("trustStore");
        if (trustStoreNode != null) {
            try {
                TrustStoreType type = TrustStoreType.valueOf(trustStoreNode.path("type").asText().toUpperCase());

                TrustStore trustStore = null;
                switch (type) {
                    case JKS:
                        trustStore = trustStoreNode.traverse(jp.getCodec()).readValueAs(JKSTrustStore.class);
                        break;
                    case PEM:
                        trustStore = trustStoreNode.traverse(jp.getCodec()).readValueAs(PEMTrustStore.class);
                        break;
                    case PKCS12:
                        trustStore = trustStoreNode.traverse(jp.getCodec()).readValueAs(PKCS12TrustStore.class);
                        break;
                }

                httpClientSslOptions.setTrustStore(trustStore);
            } catch (IllegalArgumentException iae) {}
        }

        JsonNode keyStoreNode = node.get("keyStore");
        if (keyStoreNode != null) {
            try {
                KeyStoreType type = KeyStoreType.valueOf(keyStoreNode.path("type").asText().toUpperCase());

                KeyStore keyStore = null;
                switch (type) {
                    case JKS:
                        keyStore = keyStoreNode.traverse(jp.getCodec()).readValueAs(JKSKeyStore.class);
                        break;
                    case PEM:
                        keyStore = keyStoreNode.traverse(jp.getCodec()).readValueAs(PEMKeyStore.class);
                        break;
                    case PKCS12:
                        keyStore = keyStoreNode.traverse(jp.getCodec()).readValueAs(PKCS12KeyStore.class);
                        break;
                }

                httpClientSslOptions.setKeyStore(keyStore);
            } catch (IllegalArgumentException iae) {}
        }

        return httpClientSslOptions;
    }
}
