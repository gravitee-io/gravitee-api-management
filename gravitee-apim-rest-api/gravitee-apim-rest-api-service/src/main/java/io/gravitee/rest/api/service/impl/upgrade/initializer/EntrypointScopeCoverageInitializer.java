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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Warns, once at startup, about every installed entrypoint plugin whose id has no scope decision in
 * {@link ObservabilityEntrypoints}. The observability default scope is fail-open, so such an entrypoint is
 * counted as HTTP request traffic until someone records a decision; this is where an operator learns it.
 * Only this node's plugin tree is inspected, and startup never fails on it.
 */
@Component
@CustomLog
@RequiredArgsConstructor
public class EntrypointScopeCoverageInitializer implements Initializer {

    private final EntrypointPluginQueryService entrypointPluginQueryService;

    @Override
    public boolean initialize() {
        undecidedEntrypointIds().forEach(id ->
            log.warn(
                "Entrypoint plugin [{}] has no observability scope decision: its traffic is counted as HTTP request traffic until a scope is recorded in ObservabilityEntrypoints (only this node's plugin tree was inspected)",
                id
            )
        );
        return true;
    }

    /** Installed entrypoint plugins, deployed or not, whose id the registry does not declare. */
    public List<String> undecidedEntrypointIds() {
        return entrypointPluginQueryService
            .findAll()
            .stream()
            .map(ConnectorPlugin::getId)
            .filter(id -> !ObservabilityEntrypoints.declares(id))
            .sorted()
            .toList();
    }

    @Override
    public int getOrder() {
        return InitializerOrder.ENTRYPOINT_SCOPE_COVERAGE_INITIALIZER;
    }
}
