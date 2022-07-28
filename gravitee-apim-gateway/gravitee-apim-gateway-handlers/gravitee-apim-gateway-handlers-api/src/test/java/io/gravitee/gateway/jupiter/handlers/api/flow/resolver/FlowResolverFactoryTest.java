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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.jupiter.flow.BestMatchFlowResolver;
import io.gravitee.gateway.jupiter.flow.FlowResolver;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
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
class FlowResolverFactoryTest {

    protected static final String ORGANIZATION_ID = "ORGANIZATION_ID";

    @Mock
    private ConditionFilter<Flow> filter;

    private FlowResolverFactory cut;

    @BeforeEach
    void init() {
        cut = new FlowResolverFactory(filter);
    }

    @Test
    void shouldCreateApiFlowResolver() {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        final Api api = new Api(definition);
        definition.setFlowMode(FlowMode.DEFAULT);

        final FlowResolver flowResolver = cut.forApi(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof ApiFlowResolver);
    }

    @Test
    void shouldCreateBestMatchApiFlowResolver() {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        final Api api = new Api(definition);
        definition.setFlowMode(FlowMode.BEST_MATCH);

        final FlowResolver flowResolver = cut.forApi(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof BestMatchFlowResolver);
    }

    @Test
    void shouldCreateApiPlanFlowResolver() {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        final Api api = new Api(definition);
        definition.setFlowMode(FlowMode.DEFAULT);

        final FlowResolver flowResolver = cut.forApiPlan(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof ApiPlanFlowResolver);
    }

    @Test
    void shouldCreateBestMatchApiPlanFlowResolver() {
        final io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        final Api api = new Api(definition);
        definition.setFlowMode(FlowMode.BEST_MATCH);

        final FlowResolver flowResolver = cut.forApiPlan(api);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof BestMatchFlowResolver);
    }

    @Test
    void shouldCreatePlatformFlowResolver() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final Organization organization = mock(Organization.class);
        final Api api = mock(Api.class);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getFlowMode()).thenReturn(FlowMode.DEFAULT);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);

        final FlowResolver flowResolver = cut.forPlatform(api, organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof PlatformFlowResolver);
    }

    @Test
    void shouldCreatePlatformFlowResolverWhenNoOrganization() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final Api api = mock(Api.class);

        when(organizationManager.getCurrentOrganization()).thenReturn(null);

        final FlowResolver flowResolver = cut.forPlatform(api, organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof PlatformFlowResolver);
    }

    @Test
    void shouldCreateBestMatchPlatformFlowResolver() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final Organization organization = mock(Organization.class);
        final Api api = mock(Api.class);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getFlowMode()).thenReturn(FlowMode.BEST_MATCH);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);

        final FlowResolver flowResolver = cut.forPlatform(mock(Api.class), organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof BestMatchFlowResolver);
    }
}
