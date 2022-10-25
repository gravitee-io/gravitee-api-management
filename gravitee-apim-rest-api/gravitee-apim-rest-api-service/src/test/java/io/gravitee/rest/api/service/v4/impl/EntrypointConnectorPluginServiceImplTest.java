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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.github.fge.jsonschema.main.JsonSchema;
import com.google.errorprone.annotations.DoNotMock;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EntrypointConnectorPluginServiceImplTest {

    private static final String CONNECTOR_ID = "connector-id";

    private EntrypointConnectorPluginService cut;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private EntrypointConnectorPluginManager pluginManager;

    @Before
    public void setUp() {
        cut = new EntrypointConnectorPluginServiceImpl(jsonSchemaService, pluginManager);
    }

    @Test
    public void shouldGetSubscriptionSchema() throws IOException {
        when(pluginManager.getSubscriptionSchema(CONNECTOR_ID)).thenReturn("subscriptionConfiguration");

        final String result = cut.getSubscriptionSchema(CONNECTOR_ID);

        assertThat(result).isEqualTo("subscriptionConfiguration");
    }

    @Test
    public void shouldNotGetSubscriptionSchemaBecauseOfIOException() throws IOException {
        when(pluginManager.getSubscriptionSchema(CONNECTOR_ID)).thenThrow(IOException.class);

        try {
            cut.getSubscriptionSchema(CONNECTOR_ID);
            fail("We should not go further because call should throw a TechnicalManagementException");
        } catch (TechnicalManagementException e) {
            assertThat(e.getMessage())
                .isEqualTo("An error occurs while trying to get entrypoint subscription schema for plugin " + CONNECTOR_ID);
        }
    }
}
