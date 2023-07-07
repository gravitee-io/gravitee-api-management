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
package io.gravitee.apim.plugin.apiservice.healthcheck.http.context;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckServiceConfiguration;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class HttpHealthCheckExecutionContextTest {

    @Test
    void should_not_initialize_REQUEST_ENDPOINT_key_when_config_does_not_required_it() {
        final var configuration = new HttpHealthCheckServiceConfiguration();
        configuration.setTarget("/no/absolute");
        configuration.setOverrideEndpointPath(false);
        final var deploymentContext = mock(DeploymentContext.class);
        final var cut = new HttpHealthCheckExecutionContext(configuration, deploymentContext);

        final String attribute = cut.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT);
        assertThat(attribute).isNotNull().isEqualTo(configuration.getTarget());
        final Boolean override = cut.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT_OVERRIDE);
        assertThat(override).isNotNull().isFalse();
    }

    @Test
    void should_initialize_REQUEST_ENDPOINT_key_when_config_has_absolute_target_url() {
        final var configuration = new HttpHealthCheckServiceConfiguration();
        configuration.setTarget("http://myabsolute/url");
        configuration.setOverrideEndpointPath(false);
        final var deploymentContext = mock(DeploymentContext.class);
        final var cut = new HttpHealthCheckExecutionContext(configuration, deploymentContext);

        final String attribute = cut.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT);
        assertThat(attribute).isNotNull().isEqualTo(configuration.getTarget());
        final Boolean override = cut.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT_OVERRIDE);
        assertThat(override).isNotNull().isTrue();
    }

    @Test
    void should_initialize_REQUEST_ENDPOINT_key_when_config_has_override_endpoint_to_true() {
        final var configuration = new HttpHealthCheckServiceConfiguration();
        configuration.setTarget("/no/absolute/url");
        configuration.setOverrideEndpointPath(true);
        final var deploymentContext = mock(DeploymentContext.class);
        final var cut = new HttpHealthCheckExecutionContext(configuration, deploymentContext);

        final String attribute = cut.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT);
        assertThat(attribute).isNotNull().isEqualTo(configuration.getTarget());
        final Boolean override = cut.getAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT_OVERRIDE);
        assertThat(override).isNotNull().isTrue();
    }
}
