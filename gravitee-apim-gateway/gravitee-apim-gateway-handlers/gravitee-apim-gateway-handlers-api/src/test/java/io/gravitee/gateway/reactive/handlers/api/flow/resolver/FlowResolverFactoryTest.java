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
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.BestMatchFlowResolver;
import io.gravitee.gateway.reactive.flow.FlowResolver;
import io.gravitee.gateway.reactive.platform.organization.flow.OrganizationFlowResolver;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
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
    private ConditionFilter<HttpBaseExecutionContext, Flow> filter;

    @Mock
    private AbstractBestMatchFlowSelector bestMatchFlowSelector;

    private FlowResolverFactory cut;

    @BeforeEach
    void init() {
        cut = new FlowResolverFactory(filter, bestMatchFlowSelector);
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
    void shouldCreateOrganizationFlowResolver() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);
        final ReactableOrganization reactableOrganization = mock(ReactableOrganization.class);

        when(organizationManager.getOrganization(ORGANIZATION_ID)).thenReturn(reactableOrganization);

        final FlowResolver flowResolver = cut.forOrganization(ORGANIZATION_ID, organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof OrganizationFlowResolver);
    }

    @Test
    void shouldCreatePlatformFlowResolverWhenNoOrganization() {
        final OrganizationManager organizationManager = mock(OrganizationManager.class);

        when(organizationManager.getOrganization(ORGANIZATION_ID)).thenReturn(null);

        final FlowResolver flowResolver = cut.forOrganization(ORGANIZATION_ID, organizationManager);

        assertNotNull(flowResolver);
        assertTrue(flowResolver instanceof OrganizationFlowResolver);
    }
}
