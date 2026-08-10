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
package io.gravitee.gamma.rest.core.observability.dashboard.model;

import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What a writer requires of the stored revision before its write may be applied.
 *
 * <p>Two shapes, and the type is sealed so that adding a third forces every decision site to be revisited rather than
 * silently defaulting. The distinction is deliberately not modelled as "a version, or null": an absent precondition
 * and a deliberate overwrite are opposite intents, and collapsing them is precisely how last-write-wins creeps back
 * in — a caller that forgot to state a version would inherit the behaviour of one that explicitly asked to clobber.
 *
 * @author GraviteeSource Team
 */
public sealed interface VersionPrecondition {
    /** Whether a write may proceed against {@code storedVersion}. */
    boolean isSatisfiedBy(Integer storedVersion);

    /**
     * The stored revision must be one of {@code versions}. Usually a single value; a set because HTTP's
     * {@code If-Match} accepts a list of validators and any one of them matching is enough.
     */
    record OneOf(Set<Integer> versions) implements VersionPrecondition {
        public OneOf {
            if (versions == null || versions.isEmpty()) {
                throw new InvalidDashboardException("At least one version must be stated");
            }
            versions = Set.copyOf(versions);
        }

        public static OneOf of(int version) {
            return new OneOf(Set.of(version));
        }

        /**
         * A dashboard with no stored version satisfies no version — and the null check is not decorative: the
         * immutable set this holds throws on {@code contains(null)} rather than answering false.
         */
        @Override
        public boolean isSatisfiedBy(Integer storedVersion) {
            return storedVersion != null && versions.contains(storedVersion);
        }
    }

    /**
     * Any current revision will do — an explicit overwrite. It says nothing about the dashboard <em>existing</em>:
     * that is still required, so a deliberate overwrite cannot resurrect something another author deleted.
     */
    record AnyVersion() implements VersionPrecondition {
        @Override
        public boolean isSatisfiedBy(Integer storedVersion) {
            return true;
        }
    }

    static VersionPrecondition oneOf(Set<Integer> versions) {
        return new OneOf(new LinkedHashSet<>(versions));
    }

    static VersionPrecondition version(int version) {
        return OneOf.of(version);
    }

    static VersionPrecondition anyVersion() {
        return new AnyVersion();
    }
}
