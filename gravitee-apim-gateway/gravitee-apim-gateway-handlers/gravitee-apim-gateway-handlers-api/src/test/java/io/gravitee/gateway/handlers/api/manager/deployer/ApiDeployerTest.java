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
package io.gravitee.gateway.handlers.api.manager.deployer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private ApiDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiDeployer(gatewayConfiguration, dataEncryptor);
    }

    @SneakyThrows
    @Test
    void should_decrypt_properties_on_initialize() {
        final Api api = buildApiWithProperties(new Property("key1", "value1", true), new Property("key2", "value2", false));
        when(dataEncryptor.decrypt("value1")).thenReturn("VALUE1");

        cut.initialize(api);

        assertThat(api.getDefinition().getProperties().getProperties()).containsExactly(
            new Property("key1", "VALUE1", false),
            new Property("key2", "value2", false)
        );
    }

    @SneakyThrows
    @Test
    void should_not_fail_initialize_when_an_encrypted_property_value_is_malformed() {
        // Reproduces a corrupted definition where an encrypted property holds a non-base64 value (e.g. a "***"
        // display mask persisted with encrypted=true). DataEncryptor.decrypt base64-decodes before the AES step,
        // so it throws an unchecked IllegalArgumentException rather than a GeneralSecurityException. This must not
        // escape decryptProperties: otherwise the whole API deployment fails and, on the initial sync, the node
        // stays not-ready forever (see fix #18650).
        final Api api = buildApiWithProperties(new Property("bad", "***", true), new Property("good", "value2", true));
        when(dataEncryptor.decrypt("***")).thenThrow(new IllegalArgumentException("Illegal base64 character 2a"));
        when(dataEncryptor.decrypt("value2")).thenReturn("VALUE2");

        // Must not throw.
        cut.initialize(api);

        assertThat(api.getDefinition().getProperties().getProperties()).containsExactly(
            // Malformed property is left untouched (still encrypted) instead of breaking the deployment.
            new Property("bad", "***", true),
            // Other properties are still decrypted normally.
            new Property("good", "VALUE2", false)
        );
    }

    private Api buildApiWithProperties(final Property... properties) {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        definition.setProperties(new Properties(List.of(properties)));
        return new Api(definition);
    }
}
