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

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link GuardedApiPort} for domain tests, scoping every read on the environment the way the real
 * adapter does.
 *
 * <p>An API runs the Guardians its internal flow names, here as in the store: the real adapter answers
 * {@link #findByGuardian} from the index written beside the definition, this one reads the flow itself, and
 * {@code GuardedApiPortContractTest} pins that the two agree.
 *
 * @author GraviteeSource Team
 */
public class InMemoryGuardedApiPort implements GuardedApiPort {

    private final List<GuardedApi> apis = new ArrayList<>();

    /** Runs once, right before the next internal flow write: what a concurrent caller does in that window. */
    public Runnable beforeNextUpdate = () -> {};

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
    public Page<ApiReference> findByGuardian(String environmentId, String guardianId, int pageNumber, int pageSize) {
        var running = apis
            .stream()
            .filter(api -> Objects.equals(api.environmentId(), environmentId) && api.supportsGuardians() && runs(api, guardianId))
            .map(api -> new ApiReference(api.id(), api.name()))
            .sorted(Comparator.comparing(ApiReference::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        var from = Math.min((pageNumber - 1) * pageSize, running.size());
        var content = running.subList(from, Math.min(from + pageSize, running.size()));
        return new Page<>(content, pageNumber, content.size(), running.size());
    }

    @Override
    public GuardedApi updateInternalFlow(AuditInfo auditInfo, String apiId, Flow expectedInternalFlow, Flow internalFlow) {
        var concurrently = beforeNextUpdate;
        beforeNextUpdate = () -> {};
        concurrently.run();
        var stored = findById(auditInfo.environmentId(), apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
        if (!stored.supportsGuardians()) {
            throw new ApiNotFoundException(apiId);
        }
        if (!Objects.equals(stored.internalFlow(), expectedInternalFlow)) {
            throw new ApiInternalFlowChangedException(apiId);
        }
        var updated = stored.toBuilder().internalFlow(internalFlow).build();
        apis.set(apis.indexOf(stored), updated);
        return updated;
    }

    private static boolean runs(GuardedApi api, String guardianId) {
        return api
            .guardians()
            .list()
            .stream()
            .anyMatch(enabled -> enabled.guardianId().equals(guardianId));
    }
}
