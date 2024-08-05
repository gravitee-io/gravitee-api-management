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
package io.gravitee.plugin.apiservice.healthcheck.common;

import static io.gravitee.plugin.apiservice.healthcheck.common.HealthCheckManagedEndpoint.DEFAULT_FAILURE_THRESHOLD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.health.EndpointStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HealthCheckManagedEndpointTest {

    public static final String API_ID = "apiId";
    public static final String API_NAME = "apiName";
    public static final String ENDPOINT_NAME = "endpoint_name";
    public static final long TIMESTAMP = System.currentTimeMillis();
    public static final String DEFAULT_STEP = "defaut";

    @Mock
    private Api api;

    @Mock
    private Node node;

    @Mock
    private ManagedEndpoint managedEndpoint;

    @Mock
    private HealthCheckStatus healthCheckStatus;

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private ReporterService reporterService;

    @Mock
    private AlertEventProducer alertEventProducer;

    @BeforeEach
    void init() {
        final Endpoint endpoint = new Endpoint();
        endpoint.setName(ENDPOINT_NAME);
        when(this.managedEndpoint.getDefinition()).thenReturn(endpoint);
    }

    @Test
    void should_disable_endpoint_when_status_down() {
        when(managedEndpoint.getStatus()).thenReturn(ManagedEndpoint.Status.UP);
        final HealthCheckManagedEndpoint cut = buildHCManagedEndpoint();

        final EndpointStatus endpointStatus = EndpointStatus
            .forEndpoint(API_ID, API_NAME, ENDPOINT_NAME)
            .on(TIMESTAMP)
            .step(EndpointStatus.forStep(DEFAULT_STEP).build())
            .build();

        for (int i = 0; i < DEFAULT_FAILURE_THRESHOLD; i++) {
            cut.reportStatus(false, endpointStatus);
        }

        verify(endpointManager).disable(this.managedEndpoint);
        verify(reporterService, times(DEFAULT_FAILURE_THRESHOLD)).report(any());
        verify(alertEventProducer, times(2)).send(any());
    }

    @Test
    void should_report_when_status_is_reported() {
        when(managedEndpoint.getStatus()).thenReturn(ManagedEndpoint.Status.UP);
        final HealthCheckManagedEndpoint cut = buildHCManagedEndpoint();

        final EndpointStatus endpointStatus = EndpointStatus
            .forEndpoint(API_ID, API_NAME, ENDPOINT_NAME)
            .on(TIMESTAMP)
            .step(EndpointStatus.forStep(DEFAULT_STEP).build())
            .build();

        cut.reportStatus(false, endpointStatus);
        cut.reportStatus(true, endpointStatus);
        cut.reportStatus(true, endpointStatus);

        verify(reporterService, times(3)).report(any());
        verify(alertEventProducer, times(2)).send(any());
    }

    private HealthCheckManagedEndpoint buildHCManagedEndpoint() {
        return new HealthCheckManagedEndpoint(
            api,
            node,
            managedEndpoint,
            endpointManager,
            reporterService,
            alertEventProducer,
            HealthCheckManagedEndpoint.DEFAULT_SUCCESS_THRESHOLD,
            DEFAULT_FAILURE_THRESHOLD
        );
    }
}
