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
package io.gravitee.gateway.debug.platform.manager;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.PlatformPolicyManager;
import io.gravitee.gateway.platform.manager.OrganizationEvent;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.policy.PolicyDefinition;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugOrganizationManager implements OrganizationManager, EventListener<OrganizationEvent, Organization> {

    private Organization currentOrganization;
    private final PlatformPolicyManager policyManager;

    public DebugOrganizationManager(PlatformPolicyManager policyManager, EventManager eventManager) {
        this.policyManager = policyManager;
        eventManager.subscribeForEvents(this, OrganizationEvent.class);
    }

    @Override
    public boolean register(Organization organization) {
        currentOrganization = organization;
        policyManager.setDependencies(currentOrganization.dependencies(PolicyDefinition.class));
        return true;
    }

    @Override
    public void unregister(String orgId) {
        currentOrganization = null;
    }

    @Override
    public Organization getCurrentOrganization() {
        return currentOrganization;
    }

    @Override
    public void onEvent(Event<OrganizationEvent, Organization> event) {
        switch (event.type()) {
            case REGISTER:
                register(event.content());
                break;
            case UNREGISTER:
                unregister(event.content().getId());
                break;
        }
    }
}
