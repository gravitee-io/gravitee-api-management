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
package io.gravitee.gateway.debug.organization.event;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.event.OrganizationEvent;
import io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorRegistry;
import java.util.Objects;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugOrganizationEventListener
    extends AbstractService<DebugOrganizationEventListener>
    implements EventListener<OrganizationEvent, ReactableOrganization> {

    private final OrganizationReactorRegistry v3OrganizationReactorRegistry;
    private final io.gravitee.gateway.reactive.platform.organization.reactor.OrganizationReactorRegistry organizationReactorRegistry;

    public DebugOrganizationEventListener(
        final EventManager eventManager,
        final OrganizationReactorRegistry debugV3OrganizationReactorRegistry,
        final OrganizationReactorRegistry debugOrganizationReactorRegistry
    ) {
        this.v3OrganizationReactorRegistry = debugV3OrganizationReactorRegistry;
        this.organizationReactorRegistry = debugOrganizationReactorRegistry;

        eventManager.subscribeForEvents(this, OrganizationEvent.class);
    }

    @Override
    public void onEvent(Event<OrganizationEvent, ReactableOrganization> event) {
        OrganizationEvent type = event.type();
        if (Objects.requireNonNull(type) == OrganizationEvent.REGISTER) {
            this.v3OrganizationReactorRegistry.create(event.content());
            this.organizationReactorRegistry.create(event.content());
        } else if (type == OrganizationEvent.UNREGISTER) {
            this.v3OrganizationReactorRegistry.remove(event.content());
            this.organizationReactorRegistry.remove(event.content());
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        this.v3OrganizationReactorRegistry.clear();
        this.organizationReactorRegistry.clear();
    }
}
