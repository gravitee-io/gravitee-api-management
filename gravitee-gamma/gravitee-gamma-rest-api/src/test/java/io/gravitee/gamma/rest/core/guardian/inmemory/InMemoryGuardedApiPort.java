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
package io.gravitee.gamma.rest.core.guardian.inmemory;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link GuardedApiPort} for domain tests, scoping every read on the environment the way the real
 * adapter does.
 *
 * @author GraviteeSource Team
 */
public class InMemoryGuardedApiPort implements GuardedApiPort {

    private final List<GuardedApi> apis = new ArrayList<>();

    public void givenApi(GuardedApi api) {
        apis.add(api);
    }

    public void reset() {
        apis.clear();
    }

    @Override
    public Optional<GuardedApi> findById(String environmentId, String apiId) {
        return apis
            .stream()
            .filter(api -> Objects.equals(api.id(), apiId) && Objects.equals(api.environmentId(), environmentId))
            .findFirst();
    }

    @Override
    public GuardedApi updateInternalFlow(String apiId, Flow internalFlow) {
        var stored = apis
            .stream()
            .filter(api -> Objects.equals(api.id(), apiId))
            .findFirst()
            .orElseThrow();
        var updated = stored.toBuilder().internalFlow(internalFlow).build();
        apis.set(apis.indexOf(stored), updated);
        return updated;
    }
}
