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
package io.gravitee.gateway.core.endpoint.lifecycle.impl.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.gateway.connector.ConnectorRegistry;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import io.gravitee.gateway.core.endpoint.lifecycle.impl.EndpointGroupLifecycleManager;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.node.api.configuration.Configuration;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MultiTenantAwareEndpointLifecycleManager extends EndpointGroupLifecycleManager {

    private final Logger logger = LoggerFactory.getLogger(MultiTenantAwareEndpointLifecycleManager.class);

    private final String tenant;

    public MultiTenantAwareEndpointLifecycleManager(
        Api api,
        EndpointGroup group,
        EndpointFactory endpointFactory,
        ReferenceRegister referenceRegister,
        ConnectorRegistry connectorRegistry,
        Configuration configuration,
        ObjectMapper mapper,
        String tenant
    ) {
        super(api, group, endpointFactory, referenceRegister, connectorRegistry, configuration, mapper);
        this.tenant = tenant;
    }

    @Override
    protected void doStart() throws Exception {
        logger.debug("Prepare API endpoints for tenant: {}", tenant);

        super.doStart();
    }

    @Override
    protected Predicate<Endpoint> filter() {
        return super
            .filter()
            .and(
                endpoint ->
                    (endpoint.getTenants() == null || endpoint.getTenants().isEmpty()) ||
                    (endpoint.getTenants() != null && endpoint.getTenants().contains(tenant))
            );
    }
}
