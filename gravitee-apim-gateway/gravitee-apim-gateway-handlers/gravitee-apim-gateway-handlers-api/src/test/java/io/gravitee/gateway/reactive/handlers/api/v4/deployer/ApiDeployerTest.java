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
package io.gravitee.gateway.reactive.handlers.api.v4.deployer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import java.util.List;
import javax.crypto.BadPaddingException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ApiDeployerTest {

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private final MemoryAppender memoryAppender = new MemoryAppender();

    // The generic JDK message for a wrong-key AES/PKCS5Padding failure; never carries the value.
    private static final BadPaddingException WRONG_KEY_FAILURE = new BadPaddingException(
        "Given final block not properly padded. Such issues can arise if a bad key is used during decryption."
    );

    private ApiDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiDeployer(gatewayConfiguration, dataEncryptor);
        memoryAppender.reset();
    }

    @Test
    void should_initialize_without_error_when_no_properties() {
        final Api api = new Api(io.gravitee.definition.model.v4.Api.builder().build());
        cut.initialize(api);
        assertThat(api.getDefinition().getProperties()).isNullOrEmpty();
    }

    @SneakyThrows
    @Test
    void should_decrypt_only_the_encrypted_properties_on_initialize() {
        final Api api = anApiWithProperties(
            Property.builder().key("key1").value("value1").encrypted(true).build(),
            Property.builder().key("key2").value("value2").encrypted(true).build(),
            Property.builder().key("key3").value("value3").encrypted(false).build()
        );
        when(dataEncryptor.decrypt(any())).thenAnswer(invocation -> invocation.getArguments()[0].toString().toUpperCase());

        cut.initialize(api);

        assertThat(api.getDefinition().getProperties()).containsExactly(
            Property.builder().key("key1").value("VALUE1").encrypted(false).build(),
            Property.builder().key("key2").value("VALUE2").encrypted(false).build(),
            Property.builder().key("key3").value("value3").encrypted(false).build()
        );
    }

    @SneakyThrows
    @Test
    void should_decrypt_a_dynamic_property_and_keep_it_dynamic() {
        final Api api = anApiWithProperties(
            Property.builder().key("dynamic-encrypted").value("value1").encrypted(true).dynamic(true).build(),
            Property.builder().key("dynamic-plain").value("value2").encrypted(false).dynamic(true).build(),
            Property.builder().key("manual-encrypted").value("value3").encrypted(true).dynamic(false).build()
        );
        when(dataEncryptor.decrypt(any())).thenAnswer(invocation -> invocation.getArguments()[0].toString().toUpperCase());

        cut.initialize(api);

        assertThat(api.getDefinition().getProperties()).containsExactly(
            Property.builder().key("dynamic-encrypted").value("VALUE1").encrypted(false).dynamic(true).build(),
            Property.builder().key("dynamic-plain").value("value2").encrypted(false).dynamic(true).build(),
            Property.builder().key("manual-encrypted").value("VALUE3").encrypted(false).dynamic(false).build()
        );
    }

    @SneakyThrows
    @Test
    void should_expose_the_decrypted_value_to_the_expression_language() {
        final Api api = anApiWithProperties(
            Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build(),
            Property.builder().key("plain").value("value").build()
        );
        when(dataEncryptor.decrypt("ciphertext")).thenReturn("s3cr3t");

        cut.initialize(api);

        final TemplateEngine engine = TemplateEngine.templateEngine();
        new ApiTemplateVariableProvider(api).provide(engine.getTemplateContext());

        engine.eval("{#api.properties['secret']}", String.class).test().assertValue("s3cr3t");
        engine.eval("{#api.properties['plain']}", String.class).test().assertValue("value");
    }

    @SneakyThrows
    @Test
    void should_not_fail_initialize_when_an_encrypted_property_value_is_malformed() {
        final Api api = anApiWithProperties(
            Property.builder().key("bad").value("***").encrypted(true).build(),
            Property.builder().key("good").value("value2").encrypted(true).build()
        );
        when(dataEncryptor.decrypt("***")).thenThrow(new IllegalArgumentException("Illegal base64 character 2a"));
        when(dataEncryptor.decrypt("value2")).thenReturn("VALUE2");

        cut.initialize(api);

        assertThat(api.getDefinition().getProperties()).containsExactly(
            Property.builder().key("bad").value("***").encrypted(true).build(),
            Property.builder().key("good").value("VALUE2").encrypted(false).build()
        );
    }

    @SneakyThrows
    @Test
    void should_leave_the_property_as_is_when_the_key_cannot_decrypt_it() {
        final Api api = anApiWithProperties(
            Property.builder().key("bad").value("ciphertext").encrypted(true).build(),
            Property.builder().key("good").value("value2").encrypted(true).build()
        );
        when(dataEncryptor.decrypt("ciphertext")).thenThrow(WRONG_KEY_FAILURE);
        when(dataEncryptor.decrypt("value2")).thenReturn("VALUE2");

        cut.initialize(api);

        assertThat(api.getDefinition().getProperties()).containsExactly(
            Property.builder().key("bad").value("ciphertext").encrypted(true).build(),
            Property.builder().key("good").value("VALUE2").encrypted(false).build()
        );
    }

    @SneakyThrows
    @Test
    void should_log_the_key_and_not_the_value_when_decryption_fails() {
        configureMemoryAppender();
        final Api api = anApiWithProperties(Property.builder().key("secret-key").value("stored-value").encrypted(true).build());
        when(dataEncryptor.decrypt("stored-value")).thenThrow(WRONG_KEY_FAILURE);

        cut.initialize(api);

        assertThat(memoryAppender.getLoggedEvents()).hasSize(1);
        var event = memoryAppender.getLoggedEvents().get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("secret-key").doesNotContain("stored-value");
        // Also check the attached throwable, not just the formatted message.
        assertThat(event.getThrowableProxy().getMessage()).doesNotContain("stored-value");
    }

    private void configureMemoryAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractApiDeployer.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @AfterEach
    void afterEach() {
        ((Logger) LoggerFactory.getLogger(AbstractApiDeployer.class)).detachAppender(memoryAppender);
        memoryAppender.stop();
    }

    private static Api anApiWithProperties(Property... properties) {
        return new Api(io.gravitee.definition.model.v4.Api.builder().properties(List.of(properties)).build());
    }
}
