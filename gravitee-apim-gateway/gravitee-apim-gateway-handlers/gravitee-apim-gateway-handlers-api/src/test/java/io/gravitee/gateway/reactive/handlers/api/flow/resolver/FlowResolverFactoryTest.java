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
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactive.flow.BestMatchFlowResolver;
import io.gravitee.gateway.reactive.flow.FlowResolver;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class FlowResolverFactoryTest {

    protected static final String ORGANIZATION_ID = "ORGANIZATION_ID";

    @Test
    public void shouldCreateApiFlowResolver() {
        final Api api = new Api();
        api.setFlowMode(FlowMode.DEFAULT);

        final FlowResolver flowResolver = FlowResolverFactory.forApi(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof ApiFlowResolver);
    }

    @Test
    public void shouldCreateBestMatchApiFlowResolver() {
        final Api api = new Api();
        api.setFlowMode(FlowMode.BEST_MATCH);

        final FlowResolver flowResolver = FlowResolverFactory.forApi(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof BestMatchFlowResolver);
    }

    @Test
    public void shouldCreateApiPlanFlowResolver() {
        final Api api = new Api();
        api.setFlowMode(FlowMode.DEFAULT);

        final FlowResolver flowResolver = FlowResolverFactory.forApiPlan(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof ApiPlanFlowResolver);
    }

    @Test
    public void shouldCreateBestMatchApiPlanFlowResolver() {
        final Api api = new Api();
        api.setFlowMode(FlowMode.BEST_MATCH);

        final FlowResolver flowResolver = FlowResolverFactory.forApiPlan(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof BestMatchFlowResolver);
    }

    @Test
    public void shouldCreatePlatformFlowResolver() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final Organization organization = mock(Organization.class);
        final Api api = mock(Api.class);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getFlowMode()).thenReturn(FlowMode.DEFAULT);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);

        final FlowResolver flowResolver = FlowResolverFactory.forPlatform(api, organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof PlatformFlowResolver);
    }

    @Test
    public void shouldCreatePlatformFlowResolverWhenNoOrganization() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final Api api = mock(Api.class);

        when(organizationManager.getCurrentOrganization()).thenReturn(null);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);

        final FlowResolver flowResolver = FlowResolverFactory.forPlatform(api, organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof PlatformFlowResolver);
    }

    @Test
    public void shouldCreateBestMatchPlatformFlowResolver() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final Organization organization = mock(Organization.class);
        final Api api = mock(Api.class);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getFlowMode()).thenReturn(FlowMode.BEST_MATCH);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);

        final FlowResolver flowResolver = FlowResolverFactory.forPlatform(mock(Api.class), organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof BestMatchFlowResolver);
    }
}
