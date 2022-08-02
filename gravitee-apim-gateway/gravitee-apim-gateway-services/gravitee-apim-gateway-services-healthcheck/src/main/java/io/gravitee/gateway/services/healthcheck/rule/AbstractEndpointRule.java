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
package io.gravitee.gateway.services.healthcheck.rule;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.Step;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.vertx.core.net.ProxyOptions;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractEndpointRule<T extends Endpoint> implements EndpointRule<T> {

    private final Api api;
    private final T endpoint;
    private final HealthCheckService service;
    private ProxyOptions systemProxyOptions;

    protected AbstractEndpointRule(
        final Api api,
        final T endpoint,
        final HealthCheckService service,
        final ProxyOptions systemProxyOptions
    ) {
        this.api = api;
        this.endpoint = endpoint;
        this.service = service;
        this.systemProxyOptions = systemProxyOptions;
    }

    @Override
    public Api api() {
        return api;
    }

    @Override
    public T endpoint() {
        return endpoint;
    }

    @Override
    public String schedule() {
        return service.getSchedule();
    }

    @Override
    public List<Step> steps() {
        return service.getSteps();
    }

    @Override
    public ProxyOptions getSystemProxyOptions() {
        return systemProxyOptions;
    }
}
