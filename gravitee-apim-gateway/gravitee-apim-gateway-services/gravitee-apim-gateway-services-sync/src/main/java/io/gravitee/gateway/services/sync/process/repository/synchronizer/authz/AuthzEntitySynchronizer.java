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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.gateway.services.sync.process.common.deployer.Deployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.ThreadPoolExecutor;

public class AuthzEntitySynchronizer extends AbstractAuthzReactorSynchronizer<AuthzEntityReactorDeployable> {

    private final AuthzEntityMapper mapper;
    private final DeployerFactory deployerFactory;

    public AuthzEntitySynchronizer(
        LatestEventFetcher eventsFetcher,
        AuthzEntityMapper mapper,
        DeployerFactory deployerFactory,
        AuthzEnginePort enginePort,
        AuthzScopePlacement placement,
        ThreadPoolExecutor syncFetcherExecutor,
        ThreadPoolExecutor syncDeployerExecutor
    ) {
        super(eventsFetcher, enginePort, placement, syncFetcherExecutor, syncDeployerExecutor);
        this.mapper = mapper;
        this.deployerFactory = deployerFactory;
    }

    @Override
    public int order() {
        return Order.AUTHZ_ENTITY.index();
    }

    @Override
    protected Maybe<AuthzEntityReactorDeployable> toDeploy(Event event) {
        return mapper.toDeploy(event);
    }

    @Override
    protected Maybe<AuthzEntityReactorDeployable> toUndeploy(Event event) {
        return mapper.toUndeploy(event);
    }

    @Override
    protected Deployer<AuthzEntityReactorDeployable> createDeployer() {
        return deployerFactory.createAuthzEntityDeployer();
    }

    @Override
    protected Event.EventProperties eventProperty() {
        return Event.EventProperties.AUTHZ_ENTITY_ID;
    }

    @Override
    protected EventType publishType() {
        return EventType.PUBLISH_AUTHZ_ENTITY;
    }

    @Override
    protected EventType unpublishType() {
        return EventType.UNPUBLISH_AUTHZ_ENTITY;
    }

    @Override
    protected String singularLabel() {
        return "authz entity";
    }

    @Override
    protected String pluralLabel() {
        return "authz entities";
    }
}
