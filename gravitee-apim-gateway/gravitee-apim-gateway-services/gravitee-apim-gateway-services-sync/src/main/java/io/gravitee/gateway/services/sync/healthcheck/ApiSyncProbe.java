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
package io.gravitee.gateway.services.sync.healthcheck;

import io.gravitee.gateway.services.sync.SyncService;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP Probe used to check if the gateway is ready to get calls.
 *
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSyncProbe implements Probe {

    @Autowired
    private SyncService syncService;

    @Override
    public String id() {
        return "api-sync";
    }

    @Override
    public CompletableFuture<Result> check() {
        if (syncService.isAllApisSync()) {
            return CompletableFuture.completedFuture(Result.healthy());
        }
        return CompletableFuture.completedFuture(Result.notReady());
    }

    @Override
    public boolean isVisibleByDefault() {
        return false;
    }
}
