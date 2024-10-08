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
package io.gravitee.gateway.reactive.platform.organization.flow;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.AbstractFlowResolver;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Objects;

/**
 * Allows resolving {@link Flow}s managed at platform level.
 * Api is linked to an organization and inherits from all flows defined at organization level (aka platform level).
 * The current implementation relies on the {@link OrganizationManager} to retrieve the organization of the api.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationFlowResolver extends AbstractFlowResolver<HttpBaseExecutionContext> {

    private final String organizationId;
    private final OrganizationManager organizationManager;
    private final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector;
    private Flowable<Flow> flows;
    private ReactableOrganization reactableOrganization;

    public OrganizationFlowResolver(
        final String organizationId,
        final OrganizationManager organizationManager,
        final ConditionFilter<HttpBaseExecutionContext, Flow> filter,
        final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector
    ) {
        super(filter);
        this.organizationId = organizationId;
        this.organizationManager = organizationManager;
        this.bestMatchFlowSelector = bestMatchFlowSelector;
        initFlows();
    }

    @Override
    public Flowable<Flow> resolve(final HttpBaseExecutionContext ctx) {
        return super
            .resolve(ctx)
            .compose(upstream -> {
                if (isBestMatch()) {
                    return upstream
                        .toList()
                        .flatMapMaybe(flowList ->
                            Maybe.fromCallable(() -> bestMatchFlowSelector.forPath(flowList, ctx.request().pathInfo()))
                        )
                        .toFlowable();
                } else {
                    return upstream;
                }
            });
    }

    private boolean isBestMatch() {
        return this.reactableOrganization != null && this.reactableOrganization.getFlowMode() == FlowMode.BEST_MATCH;
    }

    @Override
    public Flowable<Flow> provideFlows(HttpBaseExecutionContext ctx) {
        initFlows();
        return this.flows;
    }

    private void initFlows() {
        final ReactableOrganization refreshedReactableOrganization = organizationManager.getOrganization(organizationId);

        // Platform flows must be initialized the first time or when the organization has changed.
        if (flows == null || !Objects.equals(reactableOrganization, refreshedReactableOrganization)) {
            this.reactableOrganization = refreshedReactableOrganization;
            this.flows = provideFlows();
        }
    }

    private Flowable<Flow> provideFlows() {
        if (reactableOrganization == null || reactableOrganization.getFlows() == null || reactableOrganization.getFlows().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(reactableOrganization.getFlows().stream().filter(Flow::isEnabled).toList());
    }
}
