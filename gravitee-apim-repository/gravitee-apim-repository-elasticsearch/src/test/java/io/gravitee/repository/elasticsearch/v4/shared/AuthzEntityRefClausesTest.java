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

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzEntityRefClausesTest {

    private static final String TYPE_FIELD = "subject-type";
    private static final String ID_FIELD = "subject-id";
    private static final String ANY_ID = "any id";

    private final AuthzEntityRefClauses clauses = new AuthzEntityRefClauses();

    @Test
    void should_keep_the_plain_terms_query_for_a_bare_id() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("alice"));

        assertThat(clause).isEqualTo(plainTerms(ID_FIELD, "alice"));
    }

    @Test
    void should_select_nothing_when_the_filter_names_no_value() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of());

        assertThat(clause).isEqualTo(JsonObject.of("match_none", JsonObject.of()));
    }

    @Test
    void should_read_a_quoted_reference_as_exactly_one_type_and_id() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("docs::User::\"alice\""));

        assertThat(typedAlternatives(clause)).containsExactly("docs::User | alice");
    }

    @Test
    void should_resolve_a_namespace_of_any_depth_in_a_quoted_reference() {
        var type = "a::b::c::d::e::f::g::h::i::j::k::l::m::n::o::p::q::User";

        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of(type + "::\"x\""));

        assertThat(typedAlternatives(clause)).containsExactly(type + " | x");
    }

    @Test
    void should_never_split_inside_a_quoted_id() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"a::b\""));

        assertThat(typedAlternatives(clause)).containsExactly("User | a::b");
    }

    @Test
    void should_accept_a_quote_in_the_type_of_a_quoted_reference() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("a\"b::\"alice\""));

        assertThat(typedAlternatives(clause)).containsExactly("a\"b | alice");
    }

    @Test
    void should_unescape_a_backslash_in_a_quoted_id() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"C:\\\\temp\""));

        assertThat(typedAlternatives(clause)).containsExactly("User | C:\\temp");
    }

    @Test
    void should_keep_the_trailing_backslash_of_a_quoted_id() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"abc\\\\\""));

        assertThat(typedAlternatives(clause)).containsExactly("User | abc\\");
    }

    @Test
    void should_keep_an_unknown_escape_sequence_verbatim() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"C:\\temp\""));

        assertThat(typedAlternatives(clause)).containsExactly("User | C:\\temp");
    }

    @Test
    void should_read_a_malformed_quoted_id_as_a_literal_id() {
        var escapedClosingQuote = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"abc\\\""));
        var unescapedInnerQuote = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"a\"b\""));

        assertThat(typedAlternatives(escapedClosingQuote)).containsExactly("User | \"abc\\\"");
        assertThat(typedAlternatives(unescapedInnerQuote)).containsExactly("User | \"a\"b\"");
    }

    @Test
    void should_read_a_quoted_star_as_the_id_and_a_bare_star_as_any_id() {
        var quoted = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"*\""));
        var bare = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::*"));

        assertThat(typedAlternatives(quoted)).containsExactly("User | *");
        assertThat(typedAlternatives(bare)).containsExactly("User | " + ANY_ID);
    }

    @Test
    void should_read_the_id_from_the_first_quote_even_when_it_ends_with_a_separator() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"a::\""));

        assertThat(typedAlternatives(clause)).containsExactly("User | a::");
    }

    @Test
    void should_read_any_id_of_a_namespace_of_any_depth() {
        var type = "a::".repeat(16) + "User";

        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of(type + "::*"));

        assertThat(typedAlternatives(clause)).containsExactly(type + " | " + ANY_ID);
    }

    @Test
    void should_read_any_id_of_a_type_containing_a_quote() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("U\"ser::*"));

        assertThat(typedAlternatives(clause)).containsExactly("U\"ser | " + ANY_ID);
    }

    @Test
    void should_never_split_an_unquoted_reference_after_a_quote() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"a::b"));

        assertThat(typedAlternatives(clause)).containsExactly("User | \"a::b");
    }

    @Test
    void should_resolve_the_longest_unquoted_reference_the_mapping_can_index() {
        var type = "T".repeat(1024);
        var id = "x".repeat(1024);

        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of(type + "::" + id));

        assertThat(typedAlternatives(clause)).containsExactly(type + " | " + id);
    }

    @Test
    void should_skip_an_unquoted_split_the_mapping_cannot_index() {
        var longType = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("T".repeat(1025) + "::x"));
        var longId = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::" + "x".repeat(1025)));
        var longFirstId = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("a::" + "T".repeat(1000) + "::" + "x".repeat(1000)));

        assertThat(typedAlternatives(longType)).isEmpty();
        assertThat(typedAlternatives(longId)).isEmpty();
        assertThat(typedAlternatives(longFirstId)).containsExactly("a::" + "T".repeat(1000) + " | " + "x".repeat(1000));
    }

    @Test
    void should_read_no_type_without_a_query_budget() {
        var clause = AuthzEntityRefClauses.bareIdsOnly().matching(TYPE_FIELD, ID_FIELD, List.of("User::\"alice\""));

        assertThat(clause).isEqualTo(plainTerms(ID_FIELD, "User::\"alice\""));
    }

    @Test
    void should_try_every_split_of_an_unquoted_reference() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("docs::User::alice"));

        assertThat(typedAlternatives(clause)).containsExactly("docs | User::alice", "docs::User | alice");
    }

    @Test
    void should_cap_the_splits_of_an_unquoted_reference() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("a::".repeat(100)));

        assertThat(typedAlternatives(clause)).hasSize(16);
    }

    @Test
    void should_share_one_budget_across_the_values_of_a_query() {
        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, threeSplitReferences(1000));

        assertThat(typedAlternatives(clause)).hasSize(128);
    }

    @Test
    void should_share_one_budget_across_the_clauses_of_a_query() {
        var subjects = clauses.matching(TYPE_FIELD, ID_FIELD, threeSplitReferences(100));
        var resources = clauses.matching("resource-type", "resource-id", List.of("Doc::\"d1\""));

        assertThat(typedAlternatives(subjects)).hasSize(128);
        assertThat(resources).isEqualTo(plainTerms("resource-id", "Doc::\"d1\""));
    }

    @Test
    void should_keep_every_value_as_a_bare_id_past_the_budget() {
        var references = threeSplitReferences(1000);

        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, references);

        var bareIds = clause.getJsonObject("bool").getJsonArray("should").getJsonObject(0).getJsonObject("terms").getJsonArray(ID_FIELD);
        assertThat(bareIds.getList()).containsExactlyElementsOf(references);
    }

    @Test
    void should_resolve_the_longest_reference_the_mapping_can_index() {
        var type = "T".repeat(1024);
        var reference = type + "::\"" + "\\\"".repeat(1024) + "\"";

        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of(reference));

        assertThat(typedAlternatives(clause)).containsExactly(type + " | " + "\"".repeat(1024));
    }

    @Test
    void should_skip_a_type_or_an_id_longer_than_the_mapping_indexes() {
        var longId = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("User::\"" + "x".repeat(1025) + "\""));
        var longType = clauses.matching(TYPE_FIELD, ID_FIELD, List.of("T".repeat(1025) + "::\"x\""));

        assertThat(typedAlternatives(longId)).isEmpty();
        assertThat(typedAlternatives(longType)).isEmpty();
    }

    @Test
    void should_fall_back_to_the_plain_terms_query_for_a_value_longer_than_any_indexed_reference() {
        var value = "a::".repeat(2000);

        var clause = clauses.matching(TYPE_FIELD, ID_FIELD, List.of(value));

        assertThat(clause).isEqualTo(plainTerms(ID_FIELD, value));
    }

    private static List<String> threeSplitReferences(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> "T" + i + "::a::b::c")
            .toList();
    }

    private static JsonObject plainTerms(String field, String value) {
        return JsonObject.of("terms", JsonObject.of(field, JsonArray.of(value)));
    }

    private static List<String> typedAlternatives(JsonObject clause) {
        var alternatives = clause.containsKey("bool") ? clause.getJsonObject("bool").getJsonArray("should") : JsonArray.of(clause);
        var typed = new ArrayList<String>();
        for (var alternative : alternatives) {
            var json = (JsonObject) alternative;
            if (json.containsKey("terms")) {
                continue;
            }
            if (json.containsKey("term")) {
                typed.add(termValue(json, TYPE_FIELD) + " | " + ANY_ID);
                continue;
            }
            typed.add(typeAndId(json.getJsonObject("bool").getJsonArray("filter")));
        }
        return typed;
    }

    private static String typeAndId(JsonArray terms) {
        String type = null;
        String id = null;
        for (var entry : terms) {
            var term = ((JsonObject) entry).getJsonObject("term");
            if (term.containsKey(TYPE_FIELD)) {
                type = term.getString(TYPE_FIELD);
            } else if (term.containsKey(ID_FIELD)) {
                id = term.getString(ID_FIELD);
            } else {
                throw new AssertionError("Unexpected clause " + entry);
            }
        }
        return type + " | " + id;
    }

    private static String termValue(JsonObject clause, String field) {
        var term = clause.getJsonObject("term");
        if (!term.containsKey(field)) {
            throw new AssertionError("Unexpected clause " + clause);
        }
        return term.getString(field);
    }
}
