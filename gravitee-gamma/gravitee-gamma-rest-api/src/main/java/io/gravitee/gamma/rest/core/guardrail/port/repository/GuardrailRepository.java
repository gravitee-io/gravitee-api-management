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
package io.gravitee.gamma.rest.core.guardrail.port.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailSearchQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Guardrail store, as the domain needs it.
 *
 * <p>One port where the APIM layer had two services, a crud one and a query one: AGENTS.md §2 gives a
 * domain a single {@code Repository} port, and splitting reads from writes bought nothing here since
 * both sit on the same table.
 *
 * <p>Every read is scoped by environment on purpose — a Guardrail is defined at environment level, and
 * taking the environment as a parameter rather than trusting the caller keeps one tenant from reading
 * another's.
 *
 * @author GraviteeSource Team
 */
public interface GuardrailRepository {
    Guardrail create(Guardrail guardrail);

    /** The Guardrail with this id inside this environment, empty when it is in neither. */
    Optional<Guardrail> findByEnvironmentId(String environmentId, String guardrailId);

    /** Requested Guardrails in this environment, ignoring missing ids. */
    List<Guardrail> findByEnvironmentIdAndIds(String environmentId, Set<String> guardrailIds);

    Guardrail update(Guardrail guardrail);

    /**
     * Stores the Guardrail only if the stored one is still at {@code expectedVersion}, in one step, and
     * answers empty when it is not, or is gone.
     */
    Optional<Guardrail> updateIfVersionMatches(Guardrail guardrail, int expectedVersion);

    void updateUpdatedAt(String environmentId, String guardrailId, Instant updatedAt);

    void delete(String guardrailId);

    /**
     * Every Guardrail of this environment carrying this name, ignoring case; empty when none does.
     *
     * <p>Ignoring case because that is the rule: two Guardrails of one environment may not share a
     * name, and a difference of case is not a different name to the person reading the list.
     *
     * <p>All of them, not the first: the unique indexes compare exactly, so a store can still hold
     * "Foo" and "foo". A caller has to exclude itself from the answer, and one row cannot tell "the
     * only match is me" from "one of the matches is me".
     */
    List<Guardrail> findByEnvironmentIdAndName(String environmentId, String name);

    Page<Guardrail> search(GuardrailSearchQuery query);
}
