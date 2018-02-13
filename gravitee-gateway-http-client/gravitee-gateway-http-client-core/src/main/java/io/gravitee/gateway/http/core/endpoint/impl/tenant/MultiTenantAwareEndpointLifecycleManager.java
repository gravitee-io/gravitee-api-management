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
package io.gravitee.gateway.http.core.endpoint.impl.tenant;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.http.core.endpoint.impl.DefaultEndpointLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MultiTenantAwareEndpointLifecycleManager extends DefaultEndpointLifecycleManager {

    private final Logger logger = LoggerFactory.getLogger(MultiTenantAwareEndpointLifecycleManager.class);

    private final String tenant;

    public MultiTenantAwareEndpointLifecycleManager(String tenant) {
        this.tenant = tenant;
    }

    @Override
    protected void doStart() throws Exception {
        logger.info("Prepare API endpoints for tenant: {}", tenant);

        super.doStart();
    }

    @Override
    protected Predicate<Endpoint> filter() {
        return super.filter()
                .and(
                        endpoint -> (endpoint.getTenants() == null || endpoint.getTenants().isEmpty())
                                || (endpoint.getTenants() != null && endpoint.getTenants().contains(tenant)));
    }
}
