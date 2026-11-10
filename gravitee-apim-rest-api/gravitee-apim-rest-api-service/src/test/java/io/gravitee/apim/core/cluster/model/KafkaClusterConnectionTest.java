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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KafkaClusterConnectionTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Test
    void should_round_trip_through_json_without_derived_properties() throws Exception {
        KafkaClusterConnection connection = connection(
            """
            {"protocol":"SASL_SSL","sasl":{"mechanism":{"type":"PLAIN","username":"alice","password":"secret"}}}"""
        );
        String written = objectMapper.writeValueAsString(connection);

        assertThat(objectMapper.readTree(written).fieldNames())
            .toIterable()
            .containsExactlyInAnyOrder("crossId", "name", "bootstrapServers", "security");
        assertThat(objectMapper.readValue(written, KafkaClusterConnection.class)).isEqualTo(connection);
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "{\"protocol\":\"PLAINTEXT\"}",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"SCRAM-SHA-512\",\"username\":\"a\",\"password\":\"b\"}}}",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustStore\":{\"type\":\"PEM\",\"content\":\"cert\"},\"keyStore\":{\"type\":\"PEM\",\"certContent\":\"cert\",\"keyContent\":\"key\"}}}",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustStore\":{\"type\":\"JKS\",\"path\":\"/t.jks\",\"password\":\"p\"}}}",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustStore\":{\"type\":\"JKS\",\"content\":\"base64\",\"password\":\"p\"},\"keyStore\":{\"type\":\"PKCS12\",\"content\":\"base64\",\"password\":\"p\"}}}",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"OAUTHBEARER\",\"tokenUrl\":\"https://idp/token\",\"clientId\":\"gw\",\"clientSecret\":\"s\"}}}",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"OAUTHBEARER_TOKEN\",\"token\":\"t\"}}}",
            "{\"protocol\":\"SASL_PLAINTEXT\",\"sasl\":{\"mechanism\":{\"type\":\"GSSAPI\",\"saslJaasConfig\":\"com.sun.security.auth.module.Krb5LoginModule required;\"}}}",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"AWS_MSK_IAM\",\"saslJaasConfig\":\"software.amazon.msk.auth.iam.IAMLoginModule required;\"}}}",
        }
    )
    void should_let_message_apis_use_a_connection_the_kafka_endpoint_supports(String security) throws Exception {
        assertThat(connection(security).messageApiUnsupportedReason()).isNull();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "{\"protocol\":\"SASL_SSL\"} | It uses the SASL_SSL protocol without a SASL mechanism.",
            "{\"protocol\":\"SASL_PLAINTEXT\",\"sasl\":{\"mechanism\":{\"type\":\"NONE\"}}} | It uses the SASL_PLAINTEXT protocol without a SASL mechanism.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustAll\":true}} | Trust all is not supported: give a truststore.",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"DELEGATE_TO_BROKER\"}}} | A Message API has no client credentials to delegate to the broker.",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"GSSAPI\",\"saslJaasConfig\":\"com.sun.security.auth.module.JndiLoginModule required;\"}}} | The JAAS configuration of GSSAPI must use the login module com.sun.security.auth.module.Krb5LoginModule, not com.sun.security.auth.module.JndiLoginModule.",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"OAUTHBEARER\",\"tokenUrl\":\"file:///etc/token.jwt\"}}} | The OAUTHBEARER token endpoint must be an http or https URL.",
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"OAUTHBEARER_TOKEN\",\"token\":\"t\",\"saslExtensions\":[{\"extensionKey\":\"auth\",\"extensionValue\":\"v\"}]}}} | A SASL extension key must be made of letters only, and not be auth: auth.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"keyStore\":{\"type\":\"PEM\",\"certPath\":\"/c.pem\",\"keyPath\":\"/k.pem\"}}} | Its PEM keystore must be given by key and certificate content, not by path.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustStore\":{\"type\":\"JKS\",\"password\":\"p\"}}} | Its truststore has neither content nor path.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"keyStore\":{\"type\":\"PKCS12\",\"password\":\"p\"}}} | Its keystore has neither content nor path.",
        }
    )
    void should_say_why_message_apis_cannot_use_a_connection(String security, String reason) throws Exception {
        assertThat(connection(security).messageApiUnsupportedReason()).isEqualTo(reason);
    }

    private KafkaClusterConnection connection(String security) throws Exception {
        return objectMapper.readValue(
            "{\"crossId\":\"c\",\"name\":\"C\",\"bootstrapServers\":\"kafka:9093\",\"security\":" + security + "}",
            KafkaClusterConnection.class
        );
    }
}
