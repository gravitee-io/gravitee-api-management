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
package io.gravitee.definition.model.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.cluster.SaslMechanism.SaslExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SaslMechanismTest {

    private static final TypeReference<Map<String, Object>> RAW = new TypeReference<>() {};
    private static final String TOKEN_ENDPOINT_REASON = "The OAUTHBEARER token endpoint must be an http or https URL.";

    // Without NON_NULL inclusion: the cluster configuration is also written by mappers that keep nulls.
    private final ObjectMapper objectMapper = new ObjectMapper();

    static Stream<Map<String, Object>> mechanisms() {
        return Stream.of(
            Map.of("type", "PLAIN", "username", "alice", "password", "s3cr3t"),
            Map.of(
                "type",
                "OAUTHBEARER",
                "tokenUrl",
                "https://idp.example.com/token",
                "clientId",
                "gateway",
                "clientSecret",
                "s3cr3t",
                "scopes",
                "kafka",
                "saslExtensions",
                List.of(Map.of("extensionKey", "logicalCluster", "extensionValue", "lkc-1"))
            ),
            Map.of(
                "type",
                "OAUTHBEARER_TOKEN",
                "token",
                "eyJhbGciOiJub25lIn0.e30.",
                "saslExtensions",
                List.of(Map.of("extensionKey", "logicalCluster", "extensionValue", "lkc-1"))
            ),
            Map.of(
                "type",
                "GSSAPI",
                "saslJaasConfig",
                "com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true keyTab=\"/etc/kafka.keytab\" principal=\"gw@EXAMPLE.COM\";"
            ),
            Map.of("type", "AWS_MSK_IAM", "saslJaasConfig", "software.amazon.msk.auth.iam.IAMLoginModule required;")
        );
    }

    @ParameterizedTest
    @MethodSource("mechanisms")
    void should_keep_every_field_the_cluster_form_accepts(Map<String, Object> mechanism) {
        SaslMechanism read = objectMapper.convertValue(mechanism, SaslMechanism.class);

        assertThat(objectMapper.convertValue(read, RAW)).isEqualTo(mechanism);
    }

    @Test
    void should_not_print_the_password() {
        assertThat(new SaslMechanism("PLAIN", "alice", "s3cr3t"))
            .hasToString("SaslMechanism[type=PLAIN, username=alice, password=******]")
            .extracting(SaslMechanism::password)
            .isEqualTo("s3cr3t");
    }

    @Test
    void should_not_print_the_oauth_client_secret_nor_the_extension_values() {
        var mechanism = new SaslMechanism(
            "OAUTHBEARER",
            null,
            null,
            "https://idp.example.com/token",
            "gateway",
            "s3cr3t",
            "kafka",
            List.of(new SaslExtension("logicalCluster", "lkc-1")),
            null,
            null
        );

        assertThat(mechanism).hasToString(
            "SaslMechanism[type=OAUTHBEARER, tokenUrl=https://idp.example.com/token, clientId=gateway, clientSecret=******, scopes=kafka, saslExtensions=[logicalCluster]]"
        );
    }

    @Test
    void should_not_print_the_token_nor_the_jaas_config() {
        assertThat(new SaslMechanism("OAUTHBEARER_TOKEN", null, null, null, null, null, null, null, "eyJ.e30.", null)).hasToString(
            "SaslMechanism[type=OAUTHBEARER_TOKEN, token=******]"
        );
        assertThat(
            new SaslMechanism("GSSAPI", null, null, null, null, null, null, null, null, "Krb5LoginModule required password=\"x\";")
        ).hasToString("SaslMechanism[type=GSSAPI, saslJaasConfig=******]");
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "GSSAPI | com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true keyTab=\"/etc/kafka.keytab\";",
            "AWS_MSK_IAM | software.amazon.msk.auth.iam.IAMLoginModule required awsRoleArn=\"arn:aws:iam::1:role/kafka\";",
            "PLAIN | ",
        }
    )
    void should_accept_the_jaas_config_of_the_mechanism(String type, String saslJaasConfig) {
        SaslMechanism mechanism = jaas(type, saslJaasConfig);

        assertThat(mechanism.unsupportedReason()).isNull();
        assertThat(mechanism.resolvedUnsupportedReason()).isNull();
    }

    @Test
    void should_accept_a_login_module_after_leading_whitespace() {
        assertThat(jaas("GSSAPI", "\n  com.sun.security.auth.module.Krb5LoginModule\trequired;").resolvedUnsupportedReason()).isNull();
    }

    @Test
    void should_leave_a_jaas_config_that_is_an_expression_to_the_gateway() {
        assertThat(jaas("GSSAPI", "{#secrets.get('/vault/kafka', 'jaas')}").unsupportedReason()).isNull();
    }

    @Test
    void should_judge_a_resolved_jaas_config_even_when_it_reads_as_an_expression() {
        // What the gateway resolves "evil.Module required x=\"{#'{#'}\";" to.
        SaslMechanism mechanism = jaas("GSSAPI", "evil.Module required x=\"{#\";");

        assertThat(mechanism.resolvedUnsupportedReason()).isEqualTo(
            "The JAAS configuration of GSSAPI must use the login module com.sun.security.auth.module.Krb5LoginModule, not evil.Module."
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "GSSAPI | com.sun.security.auth.module.JndiLoginModule required; | The JAAS configuration of GSSAPI must use the login module com.sun.security.auth.module.Krb5LoginModule, not com.sun.security.auth.module.JndiLoginModule.",
            "AWS_MSK_IAM | com.sun.security.auth.module.Krb5LoginModule required; | The JAAS configuration of AWS_MSK_IAM must use the login module software.amazon.msk.auth.iam.IAMLoginModule, not com.sun.security.auth.module.Krb5LoginModule.",
        }
    )
    void should_refuse_a_jaas_config_with_another_login_module(String type, String saslJaasConfig, String reason) {
        SaslMechanism mechanism = jaas(type, saslJaasConfig);

        assertThat(mechanism.unsupportedReason()).isEqualTo(reason);
        assertThat(mechanism.resolvedUnsupportedReason()).isEqualTo(reason);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "GSSAPI | ' '", "AWS_MSK_IAM | " })
    void should_refuse_a_missing_jaas_config_and_tell_how_to_fix_a_stored_cluster(String type, String saslJaasConfig) {
        SaslMechanism mechanism = jaas(type, saslJaasConfig);

        assertThat(mechanism.unsupportedReason()).isEqualTo(
            type + " needs a JAAS configuration: give it again if this cluster was saved before it was kept."
        );
        // On the gateway, the configuration comes from a resolved cluster, not from storage.
        assertThat(mechanism.resolvedUnsupportedReason()).isEqualTo(type + " needs a JAAS configuration.");
    }

    @Test
    void should_judge_nothing_without_a_type() {
        SaslMechanism mechanism = jaas(null, "com.sun.security.auth.module.JndiLoginModule required;");

        assertThat(mechanism.unsupportedReason()).isNull();
        assertThat(mechanism.resolvedUnsupportedReason()).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "https://idp.example.com/token", "HTTP://idp.example.com/token", "' https://idp.example.com/token'" })
    void should_accept_an_http_token_endpoint(String tokenUrl) {
        SaslMechanism mechanism = tokenEndpoint(tokenUrl);

        assertThat(mechanism.unsupportedReason()).isNull();
        assertThat(mechanism.resolvedUnsupportedReason()).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "file:///etc/token.jwt", "ftp://idp.example.com/token", "not a url" })
    void should_refuse_a_token_endpoint_that_is_not_http(String tokenUrl) {
        SaslMechanism mechanism = tokenEndpoint(tokenUrl);

        assertThat(mechanism.unsupportedReason()).isEqualTo(TOKEN_ENDPOINT_REASON);
        assertThat(mechanism.resolvedUnsupportedReason()).isEqualTo(TOKEN_ENDPOINT_REASON);
    }

    @Test
    void should_leave_a_token_endpoint_that_is_an_expression_to_the_gateway() {
        assertThat(tokenEndpoint("{#secrets.get('/vault/kafka', 'url')}").unsupportedReason()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " " })
    void should_leave_a_missing_token_endpoint_to_the_cluster_form_but_refuse_it_on_the_gateway(String tokenUrl) {
        assertThat(tokenEndpoint(tokenUrl).unsupportedReason()).isNull();
        assertThat(tokenEndpoint(tokenUrl).resolvedUnsupportedReason()).isEqualTo("OAUTHBEARER needs a token endpoint.");
    }

    @ParameterizedTest
    @CsvSource({ "OAUTHBEARER", "OAUTHBEARER_TOKEN" })
    void should_accept_a_sasl_extension_key_made_of_letters(String type) {
        assertThat(extension(type, "logicalCluster").resolvedUnsupportedReason()).isNull();
    }

    @Test
    void should_accept_a_sasl_extension_key_with_the_prefix_of_its_jaas_option() {
        SaslExtension extension = new SaslExtension("extension_logicalCluster", "lkc-1");

        assertThat(extension.kafkaName()).isEqualTo("logicalCluster");
        assertThat(extension("OAUTHBEARER_TOKEN", "extension_logicalCluster").resolvedUnsupportedReason()).isNull();
    }

    @Test
    void should_ignore_a_missing_sasl_extension() {
        SaslMechanism mechanism = new SaslMechanism(
            "OAUTHBEARER_TOKEN",
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.singletonList(null),
            "eyJ.e30.",
            null
        );

        assertThat(mechanism.resolvedUnsupportedReason()).isNull();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "OAUTHBEARER | 'a\" clientSecret=\"x'",
            "OAUTHBEARER_TOKEN | logical_cluster",
            "OAUTHBEARER_TOKEN | auth",
            "OAUTHBEARER | ''",
            "OAUTHBEARER | ",
        }
    )
    void should_refuse_a_sasl_extension_key_kafka_refuses(String type, String key) {
        SaslMechanism mechanism = extension(type, key);
        String reason = "A SASL extension key must be made of letters only, and not be auth: " + key + ".";

        assertThat(mechanism.unsupportedReason()).isEqualTo(reason);
        assertThat(mechanism.resolvedUnsupportedReason()).isEqualTo(reason);
    }

    @Test
    void should_print_missing_fields_as_absent() {
        assertThat(new SaslMechanism("GSSAPI", null, null)).hasToString("SaslMechanism[type=GSSAPI]");
    }

    private static SaslMechanism jaas(String type, String saslJaasConfig) {
        return new SaslMechanism(type, null, null, null, null, null, null, null, null, saslJaasConfig);
    }

    private static SaslMechanism tokenEndpoint(String tokenUrl) {
        return new SaslMechanism("OAUTHBEARER", null, null, tokenUrl, "gateway", "s3cr3t", null, null, null, null);
    }

    private static SaslMechanism extension(String type, String key) {
        String tokenUrl = "OAUTHBEARER".equals(type) ? "https://idp.example.com/token" : null;
        return new SaslMechanism(type, null, null, tokenUrl, null, null, null, List.of(new SaslExtension(key, "v")), "eyJ.e30.", null);
    }
}
