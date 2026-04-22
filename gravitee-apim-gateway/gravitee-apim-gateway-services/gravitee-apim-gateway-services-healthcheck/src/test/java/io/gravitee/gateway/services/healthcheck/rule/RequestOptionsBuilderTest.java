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
package io.gravitee.gateway.services.healthcheck.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.healthcheck.HealthCheckRequest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.el.TemplateEngine;
import io.gravitee.node.api.Node;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestOptionsBuilderTest {

    private final Node node = mock(Node.class);
    private final TemplateEngine templateEngine = TemplateEngine.templateEngine();
    private final RequestOptionsBuilder builder = new RequestOptionsBuilder(node, templateEngine, "test-api");

    @Test
    void build_should_apply_custom_Host_header_to_RequestOptions() throws Exception {
        HealthCheckRequest hcRequest = new HealthCheckRequest("/health", HttpMethod.GET);
        hcRequest.setHeaders(List.of(new HttpHeader("Host", "custom-host.example.com")));
        HealthCheckStep step = new HealthCheckStep();
        step.setRequest(hcRequest);

        RequestOptions options = builder.build(new URL("http://actual-backend.example.com:8080/health"), step);

        assertThat(options.getHost()).isEqualTo("custom-host.example.com");
        assertThat(((SocketAddress) options.getServer()).host()).isEqualTo("actual-backend.example.com");
    }

    @Test
    void build_should_keep_target_host_when_no_custom_Host_header() throws Exception {
        HealthCheckRequest hcRequest = new HealthCheckRequest("/health", HttpMethod.GET);
        HealthCheckStep step = new HealthCheckStep();
        step.setRequest(hcRequest);

        RequestOptions options = builder.build(new URL("http://actual-backend.example.com:8080/health"), step);

        assertThat(options.getHost()).isEqualTo("actual-backend.example.com");
        assertThat(options.getServer()).isNull();
    }
}
