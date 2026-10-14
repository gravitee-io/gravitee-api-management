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
            "{\"protocol\":\"SASL_SSL\",\"sasl\":{\"mechanism\":{\"type\":\"OAUTHBEARER_TOKEN\"}}} | Message APIs do not support the SASL mechanism OAUTHBEARER_TOKEN yet.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustAll\":true}} | Trust all is not supported: give a truststore.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"trustStore\":{\"type\":\"JKS\",\"content\":\"base64\",\"password\":\"p\"}}} | Its truststore must be given by path, not by content.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"keyStore\":{\"type\":\"PEM\",\"certPath\":\"/c.pem\",\"keyPath\":\"/k.pem\"}}} | Its PEM keystore must be given by key and certificate content, not by path.",
            "{\"protocol\":\"SSL\",\"ssl\":{\"keyStore\":{\"type\":\"PKCS12\",\"content\":\"base64\",\"password\":\"p\"}}} | Its keystore must be given by path, not by content.",
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
