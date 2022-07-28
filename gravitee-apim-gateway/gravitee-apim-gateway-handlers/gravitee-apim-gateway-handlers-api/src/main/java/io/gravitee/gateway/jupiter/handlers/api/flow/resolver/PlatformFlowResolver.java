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
package io.gravitee.gateway.jupiter.handlers.api.flow.resolver;

import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.jupiter.flow.AbstractFlowResolver;
import io.gravitee.gateway.jupiter.handlers.api.definition.Api;
import io.gravitee.gateway.model.Flow;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.reactivex.Flowable;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Allows resolving {@link Flow}s managed at platform level.
 * Api is linked to an organization and inherits from all flows defined at organization level (aka platform level).
 * The current implementation relies on the {@link OrganizationManager} to retrieve the organization of the api.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PlatformFlowResolver extends AbstractFlowResolver {

    private final Api api;
    private final OrganizationManager organizationManager;
    private Flowable<Flow> flows;
    private Organization organization;

    public PlatformFlowResolver(Api api, OrganizationManager organizationManager, ConditionFilter<Flow> filter) {
        super(filter);
        this.api = api;
        this.organizationManager = organizationManager;
        initFlows();
    }

    @Override
    public Flowable<Flow> provideFlows(RequestExecutionContext ctx) {
        initFlows();
        return this.flows;
    }

    private void initFlows() {
        final Organization refreshedOrganization = organizationManager.getCurrentOrganization();

        // Platform flows must be initialized the first time or when the organization has changed.
        if (flows == null || organization != refreshedOrganization) {
            // FIXME: currently the OrganizationManager manages only one organization. It means this organization could be not related to the api (see https://github.com/gravitee-io/issues/issues/5992).
            this.organization =
                refreshedOrganization != null && Objects.equals(api.getOrganizationId(), refreshedOrganization.getId())
                    ? refreshedOrganization
                    : null;
            this.flows = provideFlows();
        }
    }

    private Flowable<Flow> provideFlows() {
        if (organization == null || organization.getFlows() == null || organization.getFlows().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(organization.getFlows().stream().filter(Flow::isEnabled).collect(Collectors.toList()));
    }
}
