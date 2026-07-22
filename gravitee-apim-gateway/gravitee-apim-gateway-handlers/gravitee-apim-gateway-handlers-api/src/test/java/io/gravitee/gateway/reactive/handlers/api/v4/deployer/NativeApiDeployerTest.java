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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactive.handlers.api.v4.NativeApi;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class NativeApiDeployerTest {

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private NativeApiDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new NativeApiDeployer(gatewayConfiguration, dataEncryptor);
    }

    @Test
    void should_set_empty_list_of_plans_on_initialize() {
        final NativeApi nativeApi = new NativeApi(io.gravitee.definition.model.v4.nativeapi.NativeApi.builder().build());
        assertThat(nativeApi.getDefinition().getPlans()).isNull();
        cut.initialize(nativeApi);
        assertThat(nativeApi.getDefinition().getPlans()).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_decrypt_properties_on_initialize() {
        final NativeApi nativeApi = new NativeApi(
            io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
                .properties(
                    List.of(
                        Property.builder().key("key1").value("value1").encrypted(true).build(),
                        Property.builder().key("key2").value("value2").encrypted(true).build(),
                        Property.builder().key("key3").value("value3").encrypted(false).build()
                    )
                )
                .build()
        );
        when(dataEncryptor.decrypt(any())).thenAnswer(invocation -> invocation.getArguments()[0].toString().toUpperCase());
        cut.initialize(nativeApi);
        assertThat(nativeApi.getDefinition().getProperties()).contains(
            Property.builder().key("key1").value("VALUE1").encrypted(false).build(),
            Property.builder().key("key2").value("VALUE2").encrypted(false).build(),
            Property.builder().key("key3").value("value3").encrypted(false).build()
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
        final NativeApi nativeApi = new NativeApi(
            io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
                .properties(
                    List.of(
                        Property.builder().key("bad").value("***").encrypted(true).build(),
                        Property.builder().key("good").value("value2").encrypted(true).build()
                    )
                )
                .build()
        );
        when(dataEncryptor.decrypt("***")).thenThrow(new IllegalArgumentException("Illegal base64 character 2a"));
        when(dataEncryptor.decrypt("value2")).thenReturn("VALUE2");

        // Must not throw.
        cut.initialize(nativeApi);

        assertThat(nativeApi.getDefinition().getProperties()).containsExactly(
            // Malformed property is left untouched (still encrypted) instead of breaking the deployment.
            Property.builder().key("bad").value("***").encrypted(true).build(),
            // Other properties are still decrypted normally.
            Property.builder().key("good").value("VALUE2").encrypted(false).build()
        );
    }
}
