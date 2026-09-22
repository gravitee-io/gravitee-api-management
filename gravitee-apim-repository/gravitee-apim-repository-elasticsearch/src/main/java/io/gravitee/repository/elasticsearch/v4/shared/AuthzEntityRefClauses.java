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
package io.gravitee.repository.elasticsearch.v4.shared;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class AuthzEntityRefClauses {

    private static final String TYPE_SEPARATOR = "::";
    private static final String QUOTED_ID_SEPARATOR = "::\"";
    private static final String ANY_ID_SUFFIX = "::*";
    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';
    // Every type and id field is a keyword with ignore_above 1024: nothing longer is indexed, so nothing longer can match.
    private static final int MAX_INDEXED_LENGTH = 1024;
    private static final int MAX_MATCHABLE_LENGTH = 3 * MAX_INDEXED_LENGTH + 4;
    private static final int MAX_SPLITS_PER_REFERENCE = 16;
    private static final int MAX_TYPED_ALTERNATIVES_PER_QUERY = 128;

    private static final AuthzEntityRefClauses BARE_IDS_ONLY = new AuthzEntityRefClauses(0);

    private int typedBudget;

    public AuthzEntityRefClauses() {
        this(MAX_TYPED_ALTERNATIVES_PER_QUERY);
    }

    private AuthzEntityRefClauses(int typedBudget) {
        this.typedBudget = typedBudget;
    }

    public static AuthzEntityRefClauses bareIdsOnly() {
        return BARE_IDS_ONLY;
    }

    public JsonObject matching(String typeField, String idField, Collection<String> references) {
        if (references == null || references.isEmpty()) {
            return JsonObject.of("match_none", JsonObject.of());
        }
        var alternatives = new JsonArray().add(JsonObject.of("terms", JsonObject.of(idField, new JsonArray(new ArrayList<>(references)))));
        for (var reference : references) {
            if (typedBudget == 0) {
                break;
            }
            for (var typeAndId : typedReadings(reference)) {
                if (typedBudget == 0) {
                    break;
                }
                alternatives.add(typeAndId.clause(typeField, idField));
                typedBudget--;
            }
        }
        if (alternatives.size() == 1) {
            return alternatives.getJsonObject(0);
        }
        return JsonObject.of("bool", JsonObject.of("should", alternatives, "minimum_should_match", 1));
    }

    private static List<TypeAndId> typedReadings(String reference) {
        if (reference.length() > MAX_MATCHABLE_LENGTH) {
            return List.of();
        }
        if (reference.length() > ANY_ID_SUFFIX.length() && reference.endsWith(ANY_ID_SUFFIX)) {
            return Optional.of(TypeAndId.anyIdOf(reference.substring(0, reference.length() - ANY_ID_SUFFIX.length())))
                .filter(TypeAndId::isIndexable)
                .stream()
                .toList();
        }
        var quoted = quotedReading(reference);
        if (quoted.isPresent()) {
            return quoted.filter(TypeAndId::isIndexable).stream().toList();
        }
        return unquotedReadings(reference);
    }

    private static Optional<TypeAndId> quotedReading(String reference) {
        int at = reference.indexOf(QUOTED_ID_SEPARATOR);
        if (at <= 0) {
            return Optional.empty();
        }
        var type = reference.substring(0, at);
        return unquoted(reference.substring(at + TYPE_SEPARATOR.length())).map(id -> new TypeAndId(type, id));
    }

    private static List<TypeAndId> unquotedReadings(String reference) {
        var readings = new ArrayList<TypeAndId>();
        int firstQuote = reference.indexOf(QUOTE);
        for (
            int at = reference.indexOf(TYPE_SEPARATOR);
            at >= 0 && readings.size() < MAX_SPLITS_PER_REFERENCE;
            at = reference.indexOf(TYPE_SEPARATOR, at + 1)
        ) {
            if (at > MAX_INDEXED_LENGTH || (firstQuote >= 0 && firstQuote < at)) {
                break;
            }
            int idStart = at + TYPE_SEPARATOR.length();
            int idLength = reference.length() - idStart;
            if (at == 0 || idLength == 0 || idLength > MAX_INDEXED_LENGTH) {
                continue;
            }
            readings.add(new TypeAndId(reference.substring(0, at), reference.substring(idStart)));
        }
        return readings;
    }

    private static Optional<String> unquoted(String id) {
        int last = id.length() - 1;
        if (last < 1 || id.charAt(0) != QUOTE || id.charAt(last) != QUOTE) {
            return Optional.empty();
        }
        var unescaped = new StringBuilder();
        for (int i = 1; i < last; i++) {
            char current = id.charAt(i);
            if (current == ESCAPE && i + 1 == last) {
                return Optional.empty();
            } else if (current == ESCAPE && isEscapable(id.charAt(i + 1))) {
                unescaped.append(id.charAt(++i));
            } else if (current == QUOTE) {
                return Optional.empty();
            } else {
                unescaped.append(current);
            }
        }
        return Optional.of(unescaped.toString());
    }

    private static boolean isEscapable(char character) {
        return character == QUOTE || character == ESCAPE;
    }

    private static JsonObject term(String field, String value) {
        return JsonObject.of("term", JsonObject.of(field, value));
    }

    private record TypeAndId(String type, String id) {
        static TypeAndId anyIdOf(String type) {
            return new TypeAndId(type, null);
        }

        boolean isIndexable() {
            return type.length() <= MAX_INDEXED_LENGTH && (id == null || id.length() <= MAX_INDEXED_LENGTH);
        }

        JsonObject clause(String typeField, String idField) {
            var typeTerm = term(typeField, type);
            if (id == null) {
                return typeTerm;
            }
            return JsonObject.of("bool", JsonObject.of("filter", JsonArray.of(typeTerm, term(idField, id))));
        }
    }
}
