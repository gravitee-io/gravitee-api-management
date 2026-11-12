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
package io.gravitee.repository.management.api.search;

import io.gravitee.repository.management.model.GuardianLifecycleState;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author GraviteeSource Team
 */
@Data
@Builder
@ToString
public class GuardianCriteria {

    /**
     * Free text matched against the Guardian name.
     */
    private String query;

    private String environmentId;

    private GuardianLifecycleState lifecycleState;

    /**
     * The fields a Guardian search may sort on, named as the domain names them. Stated here because
     * both backends have to agree: the JDBC one puts the field into its {@code ORDER BY} text, and the
     * Mongo one would otherwise sort on a field that does not exist without saying so.
     *
     * <p>{@code apiType} is absent because the definition carries {@code apiTypes} as a set: there is no
     * single value to order rows by. {@code deployedAt} is deliberately absent. The management API derives
     * it from the Guardian's publications instead of storing it, so the column holds no usable value;
     * sorting on a nullable date would also need the null ordering made explicit, the backends
     * disagreeing on where nulls land — Postgres puts them last on an ascending sort, MySQL and Mongo first.
     */
    public static final Set<String> SORTABLE_FIELDS = Set.of("name", "phase", "createdAt", "updatedAt");

    /**
     * The logical name of a sortable field, or {@link IllegalArgumentException}. Accepts the snake_case
     * spelling the backends use for their own default.
     */
    public static String requireSortableField(final String field) {
        final var logical = toCamelCase(field);
        if (logical == null || !SORTABLE_FIELDS.contains(logical)) {
            throw new IllegalArgumentException("Guardians cannot be sorted on '" + field + "'");
        }
        return logical;
    }

    private static String toCamelCase(final String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        final var out = new StringBuilder();
        var upperNext = false;
        for (final char c : field.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else {
                out.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            }
        }
        return out.toString();
    }
}
