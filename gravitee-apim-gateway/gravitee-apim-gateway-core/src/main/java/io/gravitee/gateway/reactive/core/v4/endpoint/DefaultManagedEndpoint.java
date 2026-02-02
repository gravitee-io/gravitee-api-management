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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnector;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage endpoint represents the endpoint definition and its associated instance of connector.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultManagedEndpoint implements ManagedEndpoint {

    private static final Logger log = LoggerFactory.getLogger(DefaultManagedEndpoint.class);
    private final Endpoint definition;
    private final ManagedEndpointGroup group;
    private final BaseEndpointConnector connector;
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private Status status;

    public DefaultManagedEndpoint(Endpoint definition, ManagedEndpointGroup group, BaseEndpointConnector connector) {
        this.definition = definition;
        this.group = group;
        this.connector = connector;
        this.status = Status.UP;
    }

    @Override
    public Endpoint getDefinition() {
        return definition;
    }

    @Override
    public ManagedEndpointGroup getGroup() {
        return group;
    }

    @Override
    public <T extends BaseEndpointConnector<?>> T getConnector() {
        return (T) connector;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public void incrementInFlight() {
        inFlight.incrementAndGet();
    }

    @Override
    public void decrementInFlight() {
        int next = inFlight.decrementAndGet();
        if (next < 0) {
            inFlight.set(0);
        }
    }

    @Override
    public int inFlight() {
        return inFlight.get();
    }

    @Override
    public void retireConnectionPool() {
        if (connector == null) {
            return;
        }
        try {
            Method retireMethod = connector.getClass().getMethod("retireConnectionPool");
            retireMethod.invoke(connector);
        } catch (NoSuchMethodException ignored) {
            // Connector does not support pool retirement.
        } catch (Exception exception) {
            log.debug("Failed to retire connection pool for endpoint [{}].", definition.getName(), exception);
        }
    }
}
