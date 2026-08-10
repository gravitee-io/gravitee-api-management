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
package io.gravitee.gamma.rest.core.observability.dashboard.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardNotFoundException;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardVersionConflictException;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.model.VersionPrecondition;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;

/**
 * Replaces a dashboard's author-editable content, under optimistic locking (OBS-17). Starts from the
 * existing aggregate so the server-owned fields survive the write: {@code createdAt} and
 * {@code createdBy} are carried over, {@code updatedAt} is stamped now and {@code version} is
 * incremented.
 *
 * <p>The caller must state a {@link VersionPrecondition}, and a stored revision that fails it is
 * refused with {@link DashboardVersionConflictException} rather than written over. How the
 * precondition reaches here is the REST layer's business — it arrives as {@code If-Match} and the
 * refusal leaves as {@code 412} — but the rule is a property of the operation, so it is stated as a
 * required argument rather than trusted to a single caller. There is no way to express "no
 * precondition": {@link VersionPrecondition.AnyVersion} is the deliberate overwrite, and a caller
 * that simply forgot cannot land on it by omission.
 *
 * <p>For a version-bearing precondition the comparison happens twice, and both are needed. The one
 * here reads the dashboard the caller is about to clobber, which is what the refusal has to carry.
 * The one inside {@link DashboardRepository#updateIfVersionMatches} is the actual guard: between
 * this use case's read and its write sits a window a competing save fits through, and only a
 * comparison made by the storage query itself closes it. Losing that second race is rare but not
 * impossible, hence the re-read below to answer with the state that actually won.
 *
 * <p>An overwrite skips the guard by definition, but not the existence check: it goes through
 * {@link DashboardRepository#updateIfPresent}, so it can still lose to a concurrent delete rather
 * than resurrect what another author removed.
 *
 * <p>A dashboard id from another environment throws {@link DashboardNotFoundException} — 404, not
 * 403 — via the same environment-scoped lookup as the read path, so cross-environment existence
 * cannot be probed.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class UpdateObservabilityDashboardUseCase {

    private final DashboardRepository dashboardRepository;

    public record Input(String environmentId, String dashboardId, VersionPrecondition precondition, DashboardContent content) {}

    public record Output(Dashboard dashboard) {}

    public Output execute(Input input) {
        input.content().validate();
        if (input.precondition() == null) {
            throw new InvalidDashboardException("The revision this edit is based on must be stated");
        }
        Dashboard existing = findOrThrow(input);
        if (!input.precondition().isSatisfiedBy(existing.version())) {
            throw new DashboardVersionConflictException(existing, input.precondition());
        }
        Dashboard updated = withContentOf(input, existing);

        // Exhaustive over the sealed precondition rather than an if/else, so a third kind cannot silently inherit one
        // of these two write paths. Unboxing the version is safe in the OneOf arm: it was just matched against the
        // stored one, and OneOf is satisfied by no null.
        Optional<Dashboard> written = switch (input.precondition()) {
            case VersionPrecondition.AnyVersion ignored -> dashboardRepository.updateIfPresent(updated);
            case VersionPrecondition.OneOf ignored -> dashboardRepository.updateIfVersionMatches(updated, existing.version());
        };

        return written.map(Output::new).orElseThrow(() -> new DashboardVersionConflictException(findOrThrow(input), input.precondition()));
    }

    private Dashboard findOrThrow(Input input) {
        return dashboardRepository
            .findByIdAndEnvironmentId(input.dashboardId(), input.environmentId())
            .orElseThrow(() -> new DashboardNotFoundException(input.dashboardId()));
    }

    private static Dashboard withContentOf(Input input, Dashboard existing) {
        return new Dashboard(
            existing.id(),
            existing.environmentId(),
            input.content().title(),
            input.content().description(),
            input.content().filters(),
            input.content().timeRange(),
            input.content().widgets(),
            nextVersion(existing),
            existing.createdBy(),
            existing.createdAt(),
            TimeProvider.instantNow()
        );
    }

    /**
     * A stored {@code null} is reachable only through an overwrite — the repository model still permits the value,
     * and such a dashboard carries no {@code ETag} for a caller to match on, so an overwrite is the only way to save
     * it. Starting the counter is what makes it a normally versioned dashboard from then on.
     */
    private static int nextVersion(Dashboard existing) {
        return existing.version() == null ? 1 : existing.version() + 1;
    }
}
