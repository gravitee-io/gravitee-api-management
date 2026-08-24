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
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class AuthzSchemaSynchronizer extends AbstractAuthzReactorSynchronizer<AuthzSchemaReactorDeployable> {

    private final AuthzSchemaMapper mapper;
    private final DeployerFactory deployerFactory;

    public AuthzSchemaSynchronizer(
        LatestEventFetcher eventsFetcher,
        AuthzSchemaMapper mapper,
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
        return Order.AUTHZ_SCHEMA.index();
    }

    /**
     * D7: a schema wildcard expands to the named engines only, so unlike a policy or an entity it does
     * NOT cover the bootstrap engine. Retargeting a schema from "default" to "*" must therefore still
     * evict it from the bootstrap engine, which is shared across environments.
     */
    @Override
    protected Set<String> scopesNotCoveredByWildcard() {
        return Set.of(EventBusAuthzEnginePort.DEFAULT_SCOPE);
    }

    @Override
    protected Maybe<AuthzSchemaReactorDeployable> toDeploy(Event event) {
        return mapper.toDeploy(event);
    }

    @Override
    protected Maybe<AuthzSchemaReactorDeployable> toUndeploy(Event event) {
        return mapper.toUndeploy(event);
    }

    @Override
    protected Deployer<AuthzSchemaReactorDeployable> createDeployer() {
        return deployerFactory.createAuthzSchemaDeployer();
    }

    @Override
    protected Event.EventProperties eventProperty() {
        return Event.EventProperties.AUTHZ_SCHEMA_ID;
    }

    @Override
    protected EventType publishType() {
        return EventType.PUBLISH_AUTHZ_SCHEMA;
    }

    @Override
    protected EventType unpublishType() {
        return EventType.UNPUBLISH_AUTHZ_SCHEMA;
    }

    @Override
    protected String singularLabel() {
        return "authz schema";
    }

    @Override
    protected String pluralLabel() {
        return "authz schemas";
    }
}
