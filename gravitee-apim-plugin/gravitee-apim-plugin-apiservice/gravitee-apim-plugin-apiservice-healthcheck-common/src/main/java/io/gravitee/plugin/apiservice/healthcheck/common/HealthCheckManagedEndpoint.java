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

import io.gravitee.alert.api.event.Event;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnector;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpointGroup;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import lombok.EqualsAndHashCode;

/**
 * Enhance a managed endpoint to add health check capabilities on top of it.
 * It basically manages different things:
 * <ul>
 *     <li>disable the endpoint from the endpoint manager when the health status moves to DOWN and re-enable it when it goes back to UP</li>
 *     <li>report the health check status to the reporter service</li>
 *     <li>report the health check status transition event to the alert engine</li>
 * </ul>
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HealthCheckManagedEndpoint implements ManagedEndpoint {

    public static final int DEFAULT_SUCCESS_THRESHOLD = 2;
    public static final int DEFAULT_FAILURE_THRESHOLD = 3;
    private static final String EVENT_TYPE = "ENDPOINT_HEALTH_CHECK";
    private static final String CONTEXT_NODE_ID = "node.id";
    private static final String CONTEXT_NODE_HOSTNAME = "node.hostname";
    private static final String CONTEXT_NODE_APPLICATION = "node.application";

    private static final String PROP_RESPONSE_TIME = "response_time";
    private static final String PROP_TENANT = "tenant";
    private static final String PROP_API = "api";
    private static final String PROP_ENDPOINT_NAME = "endpoint.name";
    private static final String PROP_STATUS_OLD = "status.old";
    private static final String PROP_STATUS_NEW = "status.new";
    private static final String PROP_SUCCESS = "success";
    private static final String PROP_MESSAGE = "message";

    private final Api api;
    private final Node node;

    @EqualsAndHashCode.Include
    private final ManagedEndpoint endpoint;

    private final HealthCheckStatus healthCheckStatus;
    private final EndpointManager endpointManager;
    private final ReporterService reporterService;
    private final AlertEventProducer alertEventProducer;

    public HealthCheckManagedEndpoint(
        final Api api,
        final Node node,
        final ManagedEndpoint endpoint,
        final EndpointManager endpointManager,
        final ReporterService reporterService,
        final AlertEventProducer alertEventProducer,
        final int successThreshold,
        final int failureThreshold
    ) {
        this.api = api;
        this.node = node;
        this.endpoint = endpoint;
        this.reporterService = reporterService;
        this.alertEventProducer = alertEventProducer;
        this.healthCheckStatus = new HealthCheckStatus(endpoint.getStatus(), successThreshold, failureThreshold);
        this.endpointManager = endpointManager;
    }

    /**
     * Report the result of an health check.
     *
     * @param success <code>true</code> to notify a success, <code>false</code> otherwise.
     */
    public void reportStatus(boolean success, final EndpointStatus status) {
        if (success) {
            reportSuccess(status);
        } else {
            reportFailure(status);
        }
    }

    private void reportSuccess(final EndpointStatus endpointStatus) {
        final ManagedEndpoint.Status oldStatus = healthCheckStatus.getCurrentStatus();
        final ManagedEndpoint.Status newStatus = healthCheckStatus.reportSuccess();

        if (!newStatus.isDown() && oldStatus.isDown()) {
            endpointManager.enable(endpoint);
        }
        report(endpointStatus, oldStatus, newStatus);
    }

    private void reportFailure(final EndpointStatus status) {
        final ManagedEndpoint.Status oldStatus = healthCheckStatus.getCurrentStatus();
        final ManagedEndpoint.Status newStatus = healthCheckStatus.reportFailure();

        if (newStatus.isDown() && !oldStatus.isDown()) {
            endpointManager.disable(endpoint);
        }
        report(status, oldStatus, newStatus);
    }

    private void report(
        final EndpointStatus endpointStatus,
        final ManagedEndpoint.Status oldStatus,
        final ManagedEndpoint.Status newStatus
    ) {
        endpointStatus.setTransition(oldStatus != newStatus);
        endpointStatus.setState(newStatus.code());
        endpointStatus.setAvailable(!newStatus.isDown());
        endpointStatus.setResponseTime(endpointStatus.getSteps().stream().mapToLong(Step::getResponseTime).sum());

        reporterService.report(endpointStatus);

        if (endpointStatus.isTransition() && alertEventProducer != null && !alertEventProducer.isEmpty()) {
            final Event event = Event
                .at(endpointStatus.getTimestamp())
                .context(CONTEXT_NODE_ID, node.id())
                .context(CONTEXT_NODE_HOSTNAME, node.hostname())
                .context(CONTEXT_NODE_APPLICATION, node.application())
                .type(EVENT_TYPE)
                .property(PROP_API, api.getId())
                .property(PROP_ENDPOINT_NAME, endpoint.getDefinition().getName())
                .property(PROP_STATUS_OLD, oldStatus.name())
                .property(PROP_STATUS_NEW, newStatus.name())
                .property(PROP_SUCCESS, endpointStatus.isSuccess())
                .property(PROP_TENANT, () -> node.metadata().get("tenant"))
                .property(PROP_RESPONSE_TIME, endpointStatus.getResponseTime())
                .property(PROP_MESSAGE, endpointStatus.getSteps().get(0).getMessage())
                .organization(api.getOrganizationId())
                .environment(api.getEnvironmentId())
                .build();

            alertEventProducer.send(event);
        }
    }

    @Override
    public Endpoint getDefinition() {
        return endpoint.getDefinition();
    }

    @Override
    public ManagedEndpointGroup getGroup() {
        return endpoint.getGroup();
    }

    @Override
    public <T extends BaseEndpointConnector<?>> T getConnector() {
        return endpoint.getConnector();
    }

    @Override
    public ManagedEndpoint.Status getStatus() {
        return healthCheckStatus.getCurrentStatus();
    }

    @Override
    public void setStatus(ManagedEndpoint.Status status) {
        healthCheckStatus.setCurrentStatus(status);
    }
}
