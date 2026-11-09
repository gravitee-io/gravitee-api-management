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
package io.gravitee.gamma.rest.core.guardian.port.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import java.util.Optional;

/**
 * The Guardian store, as the domain needs it.
 *
 * <p>One port where the APIM layer had two services, a crud one and a query one: AGENTS.md §2 gives a
 * domain a single {@code Repository} port, and splitting reads from writes bought nothing here since
 * both sit on the same table.
 *
 * <p>Every read is scoped by environment on purpose — a Guardian is defined at environment level, and
 * taking the environment as a parameter rather than trusting the caller keeps one tenant from reading
 * another's.
 *
 * @author GraviteeSource Team
 */
public interface GuardianRepository {
    Guardian create(Guardian guardian);

    /** The Guardian with this id inside this environment, empty when it is in neither. */
    Optional<Guardian> findByEnvironmentId(String environmentId, String guardianId);

    Guardian update(Guardian guardian);

    void delete(String guardianId);

    /**
     * The Guardian of this environment carrying this name, ignoring case; empty when none does.
     *
     * <p>Ignoring case because that is the rule: two Guardians of one environment may not share a
     * name, and a difference of case is not a different name to the person reading the list.
     */
    Optional<Guardian> findByEnvironmentIdAndName(String environmentId, String name);

    Page<Guardian> search(GuardianSearchQuery query);
}
