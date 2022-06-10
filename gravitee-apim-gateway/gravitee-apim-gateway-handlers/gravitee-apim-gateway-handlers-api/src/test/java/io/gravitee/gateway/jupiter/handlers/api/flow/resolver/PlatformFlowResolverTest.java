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

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.jupiter.core.condition.ConditionEvaluator;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
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
    private ConditionEvaluator<Flow> evaluator;

    @Mock
    private RequestExecutionContext ctx;

    @Test
    public void shouldProvidePlatformFlows() {
        final List<Flow> flows = new ArrayList<>();
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldProvideEnabledPlatformFlowsOnly() {
        final List<Flow> flows = new ArrayList<>();
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(false);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult(flow2);
    }

    @Test
    public void shouldProvideEmptyFlowsWhenNullPlatformFlows() {
        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(null);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenEmptyPlatformFlows() {
        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(emptyList());

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldProvideEmptyFlowsWhenApiOrganizationDifferentFromManagerOrganization() {
        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn("OTHER_ORGANIZATION");
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.provideFlows(ctx).test();

        obs.assertResult();
    }

    @Test
    public void shouldResolve() {
        final List<Flow> flows = new ArrayList<>();
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);
        when(evaluator.filter(eq(ctx), any())).thenAnswer(i -> i.getArgument(1));

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.resolve(ctx).test();

        obs.assertResult(flow1, flow2);
    }

    @Test
    public void shouldResolveEmptyFlowsWhenAllFlowFiltered() {
        final List<Flow> flows = new ArrayList<>();
        final Flow flow1 = mock(Flow.class);
        final Flow flow2 = mock(Flow.class);

        flows.add(flow1);
        flows.add(flow2);

        when(flow1.isEnabled()).thenReturn(true);
        when(flow2.isEnabled()).thenReturn(true);

        when(organizationManager.getCurrentOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(ORGANIZATION_ID);
        when(api.getOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(organization.getFlows()).thenReturn(flows);
        when(evaluator.filter(eq(ctx), any())).thenReturn(Flowable.empty());

        final PlatformFlowResolver cut = new PlatformFlowResolver(api, organizationManager, evaluator);
        final TestSubscriber<Flow> obs = cut.resolve(ctx).test();

        obs.assertResult();
    }
}
