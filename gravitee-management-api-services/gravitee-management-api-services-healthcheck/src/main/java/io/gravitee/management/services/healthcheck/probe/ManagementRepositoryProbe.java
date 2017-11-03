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
package io.gravitee.management.services.healthcheck.probe;

import io.gravitee.management.services.healthcheck.Probe;
import io.gravitee.management.services.healthcheck.Result;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementRepositoryProbe implements Probe {

    @Autowired
    private EventRepository eventRepository;

    @Override
    public String id() {
        return "management";
    }

    @Override
    public CompletableFuture<Result> check() {
        // Search for an event to check repository connection
        try {
            eventRepository.search(new EventCriteria.Builder()
                    .from(System.currentTimeMillis()).to(System.currentTimeMillis()).build());
            return CompletableFuture.completedFuture(Result.healthy());
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(Result.unhealthy(ex));
        }
    }
}
