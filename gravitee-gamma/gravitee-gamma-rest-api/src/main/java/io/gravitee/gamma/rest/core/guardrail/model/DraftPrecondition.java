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
package io.gravitee.gamma.rest.core.guardrail.model;

import java.util.Set;

/**
 * What a writer requires of the stored draft before its write may replace it.
 *
 * <p>Sealed, with two shapes and no "a version, or null": stating no version and asking to overwrite are
 * opposite intents, and a writer that forgot the first must not inherit the second. The observability
 * dashboards keep the same distinction for the same reason.
 *
 * @author GraviteeSource Team
 */
public sealed interface DraftPrecondition {
    /**
     * The stored draft must be at one of {@code versions}. A set, because HTTP's {@code If-Match} takes a
     * list of validators and any one of them matching is enough.
     */
    record OneOf(Set<Integer> versions) implements DraftPrecondition {
        public OneOf {
            if (versions == null || versions.isEmpty()) {
                throw new IllegalArgumentException("At least one version must be stated");
            }
            versions = Set.copyOf(versions);
        }

        /**
         * Whether a write may replace a draft stored at {@code storedVersion}. A draft stored before the counter
         * existed is at no version, so it satisfies none.
         */
        public boolean isSatisfiedBy(Integer storedVersion) {
            return storedVersion != null && versions.contains(storedVersion);
        }
    }

    /**
     * Whatever is stored may be replaced: a deliberate overwrite, and what a rollback is. The Guardrail
     * still has to exist.
     */
    record AnyVersion() implements DraftPrecondition {}

    static OneOf oneOf(Set<Integer> versions) {
        return new OneOf(versions);
    }

    static OneOf version(int version) {
        return new OneOf(Set.of(version));
    }

    static DraftPrecondition anyVersion() {
        return new AnyVersion();
    }
}
