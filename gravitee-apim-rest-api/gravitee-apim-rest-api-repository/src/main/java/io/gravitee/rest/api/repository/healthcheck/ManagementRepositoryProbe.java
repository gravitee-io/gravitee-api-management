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
package io.gravitee.rest.api.repository.healthcheck;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.management.api.InstallationRepository;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletionStage;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementRepositoryProbe implements Probe {

    @Setter
    @Autowired
    private InstallationRepository installationRepository;

    @Setter
    @Autowired
    private Vertx vertx;

    @Override
    public String id() {
        return "management-repository";
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    @Override
    public CompletionStage<Result> check() {
        return vertx
            .executeBlocking(() -> {
                try {
                    // Search for the installation information to check repository connection
                    installationRepository.find();
                    return Result.healthy();
                } catch (Exception ex) {
                    return Result.unhealthy(ex);
                }
            })
            .toCompletionStage();
    }
}
