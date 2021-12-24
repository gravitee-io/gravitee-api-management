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
package io.gravitee.repository.bridge.server.handler;

import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InstallationHandler extends AbstractHandler {

    @Autowired
    private InstallationRepository installationRepository;

    public void find(RoutingContext ctx) {
        ctx
            .vertx()
            .executeBlocking(
                (Handler<Promise<Optional<Installation>>>) promise -> {
                    try {
                        promise.complete(installationRepository.find());
                    } catch (Exception ex) {
                        LOGGER.error("Unable to search for installation", ex);
                        promise.fail(ex);
                    }
                },
                event -> handleResponse(ctx, event)
            );
    }
}
