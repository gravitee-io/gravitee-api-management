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
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import java.util.concurrent.CompletableFuture;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementRepositoryProbe implements Probe {

    @Setter
    @Autowired
    private EventRepository eventRepository;

    @Override
    public String id() {
        return "management-repository";
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    @Override
    public CompletableFuture<Result> check() {
        // Search for an event to check repository connection
        try {
            eventRepository.search(EventCriteria.builder().from(System.currentTimeMillis()).to(System.currentTimeMillis()).build());
            return CompletableFuture.completedFuture(Result.healthy());
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(Result.unhealthy(ex));
        }
    }
}
