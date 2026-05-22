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

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.edge.EdgeApi;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class EdgeApiDeployerTest {

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private EdgeApiDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new EdgeApiDeployer(gatewayConfiguration, dataEncryptor);
    }

    @Test
    void should_initialize_without_error_when_no_properties() {
        final io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi edgeApi = new io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi(
            EdgeApi.builder().build()
        );
        cut.initialize(edgeApi);
    }

    @SneakyThrows
    @Test
    void should_decrypt_properties_on_initialize() {
        final io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi edgeApi = new io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi(
            EdgeApi.builder()
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
        cut.initialize(edgeApi);
        assertThat(edgeApi.getDefinition().getProperties()).contains(
            Property.builder().key("key1").value("VALUE1").encrypted(false).build(),
            Property.builder().key("key2").value("VALUE2").encrypted(false).build(),
            Property.builder().key("key3").value("value3").encrypted(false).build()
        );
    }

    @Test
    void should_return_empty_plans() {
        final io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi edgeApi = new io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi(
            EdgeApi.builder().build()
        );
        assertThat(cut.getPlans(edgeApi)).isEmpty();
    }
}
