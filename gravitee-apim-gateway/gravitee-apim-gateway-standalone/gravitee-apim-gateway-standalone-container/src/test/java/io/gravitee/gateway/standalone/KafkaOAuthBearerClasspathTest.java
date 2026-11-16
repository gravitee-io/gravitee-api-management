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
package io.gravitee.gateway.standalone;

import static org.apache.kafka.common.config.internals.BrokerSecurityConfigs.ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaOAuthBearerClasspathTest {

    private static final String TOKEN_URL = "http://localhost:8080/token";

    private String allowedUrlsBefore;

    @BeforeEach
    void allowTokenUrl() {
        allowedUrlsBefore = System.getProperty(ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG);
        System.setProperty(ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG, TOKEN_URL);
    }

    @AfterEach
    void restoreAllowedUrls() {
        if (allowedUrlsBefore == null) {
            System.clearProperty(ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG);
        } else {
            System.setProperty(ALLOWED_SASL_OAUTHBEARER_URLS_CONFIG, allowedUrlsBefore);
        }
    }

    /**
     * kafka-clients sits in lib/ext, so the Kafka endpoint and the native Kafka reactor use it from there. Its SASL/OAUTHBEARER
     * login handler validates tokens with jose4j, which kafka-clients does not declare: without jose4j next to it, every
     * client-credentials connection fails with a NoClassDefFoundError before calling the token endpoint.
     */
    @Test
    void should_configure_the_kafka_oauthbearer_login_handler() {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
        properties.put(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, TOKEN_URL);
        Map<String, ?> configs = new ProducerConfig(properties).values();
        AppConfigurationEntry jaas = new AppConfigurationEntry(
            OAuthBearerLoginModule.class.getName(),
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
            Map.of("clientId", "client", "clientSecret", "secret")
        );

        OAuthBearerLoginCallbackHandler handler = new OAuthBearerLoginCallbackHandler();
        try {
            assertThatCode(() -> handler.configure(configs, "OAUTHBEARER", List.of(jaas))).doesNotThrowAnyException();
        } finally {
            handler.close();
        }
    }
}
