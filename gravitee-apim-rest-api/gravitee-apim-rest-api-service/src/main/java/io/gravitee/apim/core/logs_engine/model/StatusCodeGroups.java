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
package io.gravitee.apim.core.logs_engine.model;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Canonical HTTP status code group names accepted by the {@code HTTP_STATUS_CODE_GROUP} filter.
 *
 * <p>The group→bounds mapping itself belongs to the repository layer
 * ({@code io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups}), which core must not depend
 * on. Only the vocabulary is restated here, and {@code StatusCodeGroupsTest} fails if the two ever diverge.
 *
 * @author GraviteeSource Team
 */
public final class StatusCodeGroups {

    public static final Set<String> NAMES = Set.of("1XX", "2XX", "3XX", "4XX", "5XX");

    private StatusCodeGroups() {}

    /** Uppercases and trims a requested group, returning empty when it is not a known group. */
    public static Optional<String> canonicalise(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        var canonical = raw.trim().toUpperCase(Locale.ROOT);
        return NAMES.contains(canonical) ? Optional.of(canonical) : Optional.empty();
    }
}
