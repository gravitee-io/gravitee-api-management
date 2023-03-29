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

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.FlowEntity;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class PlatformFlowResolverTest {

    protected static final String ORGANIZATION_ID = "ORGANIZATION_ID";

    @Mock
    private Api api;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private Organization organization;

    @Mock
    private ConditionFilter<FlowEntity> filter;

    @Mock
    private HttpExecutionContext ctx;

    @Test
    public void shouldProvidePlatformFlows() {
        final List<FlowEntity> flows = new ArrayList<>();
        final FlowEntity flow1 = mock(FlowEntity.class);
        final FlowEntity flow2 = mock(FlowEntity.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldProvideEnabledPlatformFlowsOnly() {
        final List<FlowEntity> flows = new ArrayList<>();
        final FlowEntity flow1 = mock(FlowEntity.class);
        final FlowEntity flow2 = mock(FlowEntity.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(false);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow2);
    }

    @Test
    public void shouldProvideEmptyFlowsWhenNullPlatformFlows() {
        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(null);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenEmptyPlatformFlows() {
        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(emptyList());

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenApiOrganizationDifferentFromManagerOrganization() {
        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn("OTHER_ORGANIZATION");
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldResolve() {
        final List<FlowEntity> flows = new ArrayList<>();
        final FlowEntity flow1 = mock(FlowEntity.class);
        final FlowEntity flow2 = mock(FlowEntity.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);
        when(filter.filter(eq(ctx), any())).thenAnswer(i -> Maybe.just(i.getArgument(1)));

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.resolve(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldResolveEmptyFlowsWhenAllFlowFiltered() {
        final List<FlowEntity> flows = new ArrayList<>();
        final FlowEntity flow1 = mock(FlowEntity.class);
        final FlowEntity flow2 = mock(FlowEntity.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);
        when(filter.filter(eq(ctx), any())).thenReturn(Maybe.empty());

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, filter);
        final TestSubscriber<FlowEntity> obs = cut.resolve(ctx).test();

        obs.assertResult();
    }
}
